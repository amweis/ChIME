#ifndef STRUCTURE_H
#define STRUCTURE_H
#include <jni.h>

//#define DEBUG
#ifdef DEBUG
#include <android/log.h>

# define  LOG_TAG "wordlib"
# define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
# define  LOGV(...)  __android_log_print(ANDROID_LOG_VERBOSE,LOG_TAG,__VA_ARGS__)
# define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#else

# define  LOGI(...)  
# define  LOGV(...)
# define  LOGE(...) 

#endif

typedef struct _text_link
{
	jbyte *content;
	struct _text_link *next;
} text_link;

typedef struct _text_leaf
{
	jbyte key;
	text_link *text;
	struct _text_leaf *children;
	struct _text_leaf *sibling;
} text_leaf;

typedef struct _stack_container
{
	void *element;
	struct _stack_container* next;
} stack_container;

static text_leaf* alloc_text_leaf();
static void free_text_leaf(text_leaf *leaf);
static text_link* alloc_text_link();
static void free_text_link(text_link *link);
static stack_container* push_stack(stack_container* sc, void *cont); 
static stack_container* pop_stack(stack_container *sc); 
static int free_stack(stack_container *sc); 
/** added @ 2012/09/10 **/
static void free_more_children_stack ();
static void init_more_children_stack ();
static jint get_more_text_len (jint toget);
static char* get_more_text (jint toget);

jint Java_com_easy_ChIME_CTextGen_buildTextDict(JNIEnv* env, jobject obj,
		jbyteArray arr, jint len);

jint Java_com_easy_ChIME_CTextGen_cleanInput (JNIEnv* env, jobject obj);
jboolean Java_com_easy_ChIME_CTextGen_widthVisit (JNIEnv* env, jobject obj, jint code);
jboolean Java_com_easy_ChIME_CTextGen_depthVisit (JNIEnv* env, jobject obj, jint code);
jstring Java_com_easy_ChIME_CTextGen_getInputText(JNIEnv* env, jobject obj);
jstring Java_com_easy_ChIME_CTextGen_getMoreText (JNIEnv* env, jobject obj, jint maxSum);
void Java_com_easy_ChIME_CTextGen_nativeHandleDelete (JNIEnv* env, jobject obj);
void Java_com_easy_ChIME_CTextGen_releaseDict (JNIEnv* env, jobject obj);

#endif
