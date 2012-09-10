#include "structure.h"
#include <string.h>
#include <stdlib.h>

/** data declaration section */

static text_leaf *root_tree = NULL;
/**root_stack is a stack of text_leaf stack*/
static stack_container *root_stack = NULL;
/** more_children_stack is used to store current children leafs of current matched
  * it will be initialized when more text wanted, and released when new code matched
  */
static stack_container *more_children_stack = NULL;
/** more_text_stack is used to store the visited leafs of more_children_stack and 
  * it's text will be presented to caller. it is initialized when visiting more_children_stack
  * and will be released after it's content has been read.
  */
static stack_container *more_text_stack = NULL;

/** code section */

#ifdef DEBUG

void output_text_leaf (text_leaf *leaf)
{
	if (leaf == NULL) {
		return;
	}
	text_link *link = leaf->text;
	LOGV ("now begin output_text_leaf %d ...", leaf->key);
	if (leaf->children) {
		LOGV ("+++leaf's child is %d\n", leaf->children->key);
	}
	if (leaf->sibling) {
		LOGV ("+++leaf' sibling is %d\n", leaf->sibling);
	}
	LOGV ("+++leaf[%c]' text = %d", leaf->key, link);
	while (link) {
		if (link->content) {
			LOGV ("  ---text = %s", (char*)link->content);
		}
		link = link->next;
	}
	LOGV ("now end output_text_leaf ...");
}

#endif


/** alloc_text_leaf malloc and set the content to null **/
static text_leaf* alloc_text_leaf() {
	text_leaf *leaf = (text_leaf*) malloc(sizeof(text_leaf));
	if (leaf != NULL) {
		memset(leaf, 0, sizeof(text_leaf));
	}
#ifdef DEBUG
	if (leaf == NULL) {
		LOGV ("alloc_text_leaf malloc leaf is null, no space");
	}
#endif
	return leaf;
}

static void free_text_leaf(text_leaf *leaf) {
	stack_container *stack = NULL, *tmpstck = NULL;
	while (leaf) {
		/** store & break the sibling **/
		if (leaf->sibling) {
			tmpstck = push_stack (stack, leaf->sibling);
			if (tmpstck != NULL) {
				stack = tmpstck;
			}

		}
		/** store & break the children **/
		if (leaf->children) {
			tmpstck = push_stack (stack, leaf->children);
			if (tmpstck != NULL) {
				stack = tmpstck;
			}
		}
		/** now free the node itself **/
		if (leaf->text) {
			free_text_link (leaf->text);
		}
		free (leaf);
		if (stack != NULL) {
			leaf = (text_leaf*)stack->element;
			stack = pop_stack (stack);
		} else {
			break;
		}
	}
}

static text_link* alloc_text_link() {
	text_link *link = (text_link*) malloc(sizeof(text_link));
	if (link != NULL) {
		memset(link, 0, sizeof(text_link));
	}
#ifdef DEBUG
	if (link == NULL) {
		LOGV ("alloc_text_link malloc link is null, no space");
	}
#endif
	return link;
}

static void free_text_link(text_link *link) {
	text_link *temp = link;
	while (temp != NULL) {
		link = temp->next;
		if (temp->content != NULL) {
			free(temp->content);
		}
		free(temp);
		temp = link;
	}
}

static int whiteSpace(jbyte c) {
	switch (c) {
	case ' ':
	case '\t':
	case '\r':
	case '\n':
		return 1;
	default:
		return 0;
	}
}

/**
 * find_child: find node's child which match the keys
 * @node the node which should be checked
 * make sure the node is not null before call this methods
 */
static text_leaf* find_child(text_leaf *node, jbyte key) {
	text_leaf *children = node->children;
	while (children != NULL) {
		if (children->key == key) {
			return children;
		} else {
			children = children->sibling;
		}
	}
	return NULL;
}

/** @leaf must not be null while call this method **/
static text_leaf* add_child(text_leaf *leaf, jbyte key) {
	text_leaf *child = alloc_text_leaf();
	if (child != NULL) {
		child->key = key;
		child->sibling = leaf->children;
		leaf->children = child;
	}
	return child;
}
/** the input parameters must not be null */
static void add_word(text_leaf *node, jbyte *start, jbyte *end) {
	if (start < end) {
		text_link* link = alloc_text_link();
		if (link == NULL) {
			return;
		}
		jsize len = end - start;
		jbyte *text = (jbyte*) malloc(len + 1);
		if (text == NULL) {
			free(link);
			return;
		}
		memcpy(text, start, len);
		text[len] = 0;
		link->content = text;
		link->next = node->text;
		node->text = link;
	}
}

static jbyte* skipWhiteSpace(jbyte *start, jbyte* end) {
	while (start < end && whiteSpace(*start)) {
		++start;
	}
	return start;
}

static void build_text_leaf(text_leaf *leaf, jbyte *start, jbyte *end) {
	text_leaf *child = leaf;
	jbyte *word = NULL;
	/** first build keys (character till white space) **/
	while (start < end && child) {
		if (whiteSpace(*start)) {
			break;
		} else {
			text_leaf *tmp = find_child(child, *start);
			if (NULL == tmp) {
				tmp = add_child(child, *start);
			}
			child = tmp;
		}
		start += 1;
	}
	if (NULL == child) {
		return;
	}
	/* now build and */
	start = skipWhiteSpace(start, end);
	for (word = start; word < end; ++word) {
		if (whiteSpace(*word)) {
			add_word(child, start, word);
			word = skipWhiteSpace(word, end);
			start = word;
		}
	}
	add_word(child, start, word);
}

static jbyte* build_text_tree(jbyte *buf, jbyte *end) {
	/** if tree can not be initialized, then return the end of the buf \
      so that we consider the buf has been done **/
	if (root_tree == NULL && (root_tree = alloc_text_leaf()) == NULL) {
		return end;
	}
	while (buf < end) {
		buf = skipWhiteSpace(buf, end);
		jbyte *nline = (jbyte*) memchr(buf, '\r', end - buf);
		if (nline == NULL) {
			nline = (jbyte*) memchr(buf, '\n', end - buf);
		}
		if (nline != NULL) {
			build_text_leaf(root_tree, buf, nline);
		} else {
			break;
		}
		buf = nline + 1;
	}
	return buf;
}


/**
 */
jint Java_com_easy_ChIME_CTextGen_buildTextDict(JNIEnv* env, jobject obj,
		jbyteArray arr, jint len) {
#ifdef __cplusplus
	jbyte *buf = env->GetByteArrayElements(arr, NULL);
#else
	jbyte *buf = (*env)->GetByteArrayElements(env, arr, NULL);
#endif
	if (buf == NULL) {
		return 0;
	}
	jbyte *end = buf + len;
	buf = build_text_tree(buf, end);
#ifdef __cplusplus
	env->ReleaseByteArrayElements(arr, buf, JNI_ABORT);
#else
	(*env)->ReleaseByteArrayElements(env, arr, buf, JNI_ABORT);
#endif
	return end - buf;
}

jint Java_com_easy_ChIME_CTextGen_cleanInput (JNIEnv* env, jobject obj)
{
	jint result = 0;
	if (root_stack) {
		result = free_stack (root_stack);
		root_stack = NULL;
	}
	return result;
}

static stack_container* push_stack(stack_container* sc, void *cont) {
	stack_container *container = (stack_container*) malloc(
			sizeof(stack_container));
	if (container != NULL) {
		container->next = sc;
		container->element = cont;
	}
	return container;
}

static stack_container* pop_stack(stack_container *sc) {
	stack_container *result = sc->next;
	free(sc);
	return result;
}

static int free_stack(stack_container *sc) {
	int result = 0;
	while (sc != NULL) {
		sc = pop_stack(sc);
		++result;
	}
	return result;
}

static int isnotz(jint code) {
	return code - 'z';
}

static stack_container* find_root_tree (jint code)
{
	text_leaf *matched = NULL;
	stack_container *top = NULL;
	if (root_tree != NULL) {
		matched = find_child(root_tree, (jbyte) code);
		if (matched != NULL) {
			top = push_stack(NULL, matched);
			if (top != NULL) {
				root_stack = push_stack(NULL, top);
			}
			if (root_stack == NULL) {
				free(top);
			}
		}
	}
	return root_stack;
}

/**
 * width_visit_5stroke method used for 5 stroke input method;
 * be carefull in deal with key 'z'
 */
static stack_container* width_visit(jint code) {
	if (!root_stack) {
		return find_root_tree (code);
	}
	text_leaf *matched = NULL; /** matched is a text leaf which match the input code **/
	/** top is the top content of root_tree, and the array of matched nodes*/
	stack_container *top = NULL;
	/** matched_children is the top of children which matches the input code **/
	stack_container *matched_children = NULL;

	top = (stack_container*) root_stack->element;
	/** find each content **/
	while (top != NULL) {
		matched = (text_leaf*) top->element;
		if (isnotz(code)) {
			matched = find_child(matched, (jbyte) code);
			if (matched != NULL) {
				matched_children = push_stack(matched_children, matched);
			}
		} else {
			for (matched = matched->children; matched != NULL; matched =
					matched->sibling) {
				matched_children = push_stack(matched_children, matched);
			}
		}
		top = top->next;
	}
	/** now set the top of root_stack **/
	if (matched_children) {
		top = push_stack(root_stack, matched_children);
	} else {
		return NULL;
	}
	if (top == NULL) {
		free_stack(matched_children);
	} else {
		root_stack = top;
		/** new code matched; clean children of current top **/
		free_more_children_stack ();
	}
	return top;
}



static stack_container* slight_match_children(stack_container* sc,
		text_leaf *leaf, jbyte code) {
	stack_container *childrens = NULL;
	stack_container *tmp_contn = NULL;
	stack_container *result = sc;
	text_leaf *matched = NULL;
	while (leaf != NULL) {
		matched = find_child(leaf, code);
		if (matched != NULL) {
			sc = push_stack(result, matched);
			if (sc == NULL) {
				free_stack(childrens);
				return result;
			}
			result = sc;
			if (childrens) {
				leaf = (text_leaf*) childrens->element;
				childrens = pop_stack(childrens);
			} else {
				break;
			}
		} else if (leaf->children) {
			leaf = leaf->children;
			if (leaf->sibling) {
				tmp_contn = push_stack(childrens, leaf->sibling);
				if (tmp_contn == NULL) {
					free_stack(childrens);
					return result;
				} else {
					childrens = tmp_contn;
				}
			}
		} else if (childrens) {
			leaf = (text_leaf*) childrens->element;
			childrens = pop_stack(childrens);
		} else {
			break;
		}
	}
	return result;
}

/**
 * deep visit the trees
 * now implementation is that when an exact code is found, then return it.
 * I don't know if it is need to enum all nodes which contains such child key.
 * Maybe I will do it later. 12th July, 2012.
 */

static stack_container* deep_visit(jint code) {
	if (!root_stack) {
		return find_root_tree (code);
	}
	/** top is the top content of root_tree, and the array of matched nodes*/
	stack_container *top = NULL;
	stack_container *matched_children = NULL; /** top to be push **/
	top = (stack_container*) root_stack->element;
	while (top != NULL) {
		matched_children = slight_match_children(matched_children,
				(text_leaf*) top->element, (jbyte) code);
		top = top->next;
	}
	if (matched_children) {
		top = push_stack(root_stack, matched_children);
	} else {
		return NULL;
	}
	if (top == NULL) {
		free_stack(matched_children);
	} else {
		root_stack = top;
		/** new code matched; clean children of current top **/
		free_more_children_stack ();
	}
	return top;
}

jboolean Java_com_easy_ChIME_CTextGen_widthVisit (JNIEnv* env, jobject obj, jint code)
{
	return width_visit (code) ? 1 : 0;
}

jboolean Java_com_easy_ChIME_CTextGen_depthVisit (JNIEnv* env, jobject obj, jint code)
{
	return deep_visit (code) ? 1 : 0;
}

jint calcuTextLength ()
{
	stack_container *cont = NULL;
	text_leaf *leaf = NULL;
	text_link *link = NULL;
	jint result = 0;
	if (root_stack == NULL) {
		return 0;
	}
	cont = (stack_container*)root_stack->element;
	while (cont != NULL) {
		leaf = (text_leaf*)cont->element;
		if (leaf != NULL && leaf->text) {
			link = (text_link*)leaf->text;
			while (link != NULL) {
				if (link->content != NULL) {
					result += strlen ((char*)link->content) + 1;
				}
				link = link->next;
			}
		}
		cont = cont->next;
	}
	return result;
}

jint cpyInputText (char *buf, jint len)
{
	stack_container *cont = NULL;
	text_leaf *leaf = NULL;
	text_link *link = NULL;
	jint result = 0;
	jint contlen = 0;
	if (root_stack == NULL) {
		return 0;
	}
	cont = (stack_container*)root_stack->element;
	while (cont != NULL) {
		leaf = (text_leaf*)cont->element;
		if (leaf != NULL && leaf->text) {
			link = (text_link*)leaf->text;
			while (link != NULL) {
				if (link->content != NULL) {
					contlen = strlen ((char*)link->content);
					if (result + contlen < len) {
						memcpy (buf, link->content, contlen);
						*(buf + contlen) = '\n';
						buf += contlen + 1;
						result += contlen + 1;
					}
				}
				link = link->next;
			}
		}
		cont = cont->next;
	}
	return result;

}


jstring Java_com_easy_ChIME_CTextGen_getInputText (JNIEnv* env, jobject obj)
{

	jint len = calcuTextLength ();
	char *buf = NULL;
	jstring result = NULL;
	if (len > 0) {
		buf = (char*)malloc (len + 1);
		if (buf != NULL) {
			len = cpyInputText (buf, len);
			*(buf + len) = 0;
#ifdef __cplusplus
			result = env->NewStringUTF (buf);
#else
			result = (*env)->NewStringUTF (env, buf);
#endif
			free (buf);
		}
	}
	return result;
}


void Java_com_easy_ChIME_CTextGen_nativeHandleDelete (JNIEnv* env, jobject obj)
{
#ifdef DEBUG
	LOGV ("in handleDelete root_stack = %d", root_stack);
#endif
	if (root_stack) {
		root_stack = pop_stack (root_stack);
	}
#ifdef DEBUG
	LOGV ("out handleDelete root_stack = %d", root_stack);
#endif
}

void Java_com_easy_ChIME_CTextGen_releaseDict (JNIEnv* env, jobject obj)
{
	if (root_tree) {
		free_text_leaf (root_tree);
		root_tree = NULL;
	}
}


jstring Java_com_easy_ChIME_CTextGen_getMoreText (JNIEnv* env, jobject obj, jint maxSum)
{
	if (more_children_stack == NULL) {
		init_more_children_stack ();
	}
	char *buf = get_more_text (maxSum);
	jstring result = NULL;
	if (buf) {
#ifdef __cplusplus
		result = env->NewStringUTF (buf);
#else
		result = (*env)->NewStringUTF (env, buf);
#endif
		free (buf);
	}
	return result;
}

static int safed_push_children_stack (void *cont)
{
	stack_container *head = push_stack (more_children_stack, cont);
	if (head) {
		more_children_stack = head;
		return 1;
	}
	return 0;
}

static void free_more_children_stack ()
{
	if (more_children_stack) {
		free_stack (more_children_stack);
		more_children_stack = NULL;
	}
}

static void init_more_children_stack ()
{
	
	if (root_stack) {
		stack_container *cont = (stack_container*)root_stack->element;
		while (cont != NULL) {
			/** get text_leaf's first child and store **/
			text_leaf *leaf = (text_leaf*)cont->element;
			if (leaf->children) {
				safed_push_children_stack (leaf->children);
			}
			/** move next **/
			cont = cont->next;
		}
	}
}


static jint get_more_text_len (jint toget)
{
	jint len = 0;
	jint count = 0;
	if (!more_children_stack) {
		return len;
	}
	text_leaf *leaf = (text_leaf*)more_children_stack->element;
	more_children_stack = pop_stack (more_children_stack);

	while (leaf && count < toget) {
		text_link *link = leaf->text;
		/** copy a pointer of current node to more_text_stack **/
		if (link) {
			stack_container *head = push_stack (more_text_stack, leaf);
			if (head) {
				more_text_stack = head;
			} else {
				return len;
			}
			/** get current nodes text count and length **/
			while (link != NULL) {
				len += strlen ((char*)link->content) + 1;
				count += 1;
				link = link->next;
			}
		}
		if (count < toget) {
			/** store sibling and visit children **/
			if (leaf->children) {
				if (leaf->sibling) {
					safed_push_children_stack (leaf->sibling);
				}
				leaf = leaf->children;
			} else if (leaf->sibling) {
				leaf = leaf->sibling;
			} else if (more_children_stack) {
				leaf = (text_leaf*)more_children_stack->element;
				more_children_stack = pop_stack (more_children_stack);
			} else {
				leaf = NULL;
			}
		} else {
			if (leaf->sibling) {
				safed_push_children_stack (leaf->sibling);
			}
			if (leaf->children) {
				safed_push_children_stack (leaf->children);
			}
		}
	}
	return len;
}

static char* get_more_text (jint toget)
{
	char *buf = NULL;
	jint len = get_more_text_len (toget);
	if (len > 0) {
		buf = (char*)malloc (len + 1);
	}
	if (buf) {
		len = 0;
		/** here more_text_stack must not empty **/
		while (more_text_stack) {
			text_leaf *leaf = (text_leaf*)more_text_stack->element;
			text_link *link = leaf->text;
			while (link != NULL) {
				jint cpd = strlen ((char*)link->content);
				memcpy (buf + len, link->content, cpd);
				len += cpd;
				*(buf + len) = '\n';
				len += 1;
				link = link->next;
			}
			more_text_stack = pop_stack (more_text_stack);
		}
		*(buf + len) = 0;
	}
	return buf;
}

