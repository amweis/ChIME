#include "../jni/textbuilder.c"
#include <iostream>
#include <cppunit/extensions/HelperMacros.h>
#include <cppunit/extensions/TestFactoryRegistry.h>
#include <cppunit/ui/text/TestRunner.h>


using namespace std;

class IMETestFixture : public CppUnit::TestFixture
{
	CPPUNIT_TEST_SUITE(IMETestFixture);
//	CPPUNIT_TEST(whiteSpace);
//	CPPUNIT_TEST(find_child);
//	CPPUNIT_TEST(skipwhiteSpace);
	CPPUNIT_TEST(width_visit);
	CPPUNIT_TEST(deep_visit);
	//CPPUNIT_TEST(free_text_leaf);
	//CPPUNIT_TEST (getMoreText);
	CPPUNIT_TEST_SUITE_END();
public:
	void setUp () {
		const char *text = "aba ¹¤\naca ÈË\nabab ÄÅ";
		::build_text_tree ((jbyte*)text, (jbyte*)text + strlen (text));
		cout << "setUp exit ..." << endl;
	}

	void tearDown () {
		::free_text_leaf (root_tree);
		::free_stack (root_stack);
		root_stack = NULL;
		cout << "tearDown exit ..." << endl;
	}

	void whiteSpace () {
		CPPUNIT_ASSERT (::whiteSpace (' '));
		CPPUNIT_ASSERT (::whiteSpace ('\r'));
		CPPUNIT_ASSERT (::whiteSpace ('\n'));
		CPPUNIT_ASSERT (::whiteSpace ('\t'));
		CPPUNIT_ASSERT (::whiteSpace ('a') == 0);
	}

	void output_text (text_link *link, char key) {
		while (link != NULL) {
			if (link->content != NULL) {
				cout << "find " << key  << ((char*)link->content) << endl;
			}
			link = link->next;
		}
	}
	void find_child () {
		text_leaf *leaf = ::find_child (root_tree, 'a');
		CPPUNIT_ASSERT (leaf != NULL);
		CPPUNIT_ASSERT (leaf->text != NULL);
		output_text(leaf->text, 'a');
		leaf = ::find_child (leaf, 'b');
		CPPUNIT_ASSERT (leaf != NULL);
		CPPUNIT_ASSERT (leaf->text != NULL);
		output_text(leaf->text, 'b');
		leaf = ::find_child (leaf, 'c');
		CPPUNIT_ASSERT (leaf == NULL);
		leaf = ::find_child (root_tree, 'b');
		CPPUNIT_ASSERT (leaf != NULL);
		CPPUNIT_ASSERT (leaf->text == NULL);
		leaf = ::find_child (leaf, 'c');
		CPPUNIT_ASSERT (leaf != NULL);
		output_text (leaf->text, 'c');
	}
	void skipwhiteSpace () {
		const char *text = " \r\t\n abc";
		char *skiped = (char*)::skipWhiteSpace ((jbyte*)text, (jbyte*)text + strlen (text));
		CPPUNIT_ASSERT (*skiped == 'a');
	}
	void width_visit () {
		cout << "now width visit ..." << endl;
		stack_container *cont = ::width_visit ('a');
		cont = ::width_visit ('c');
		CPPUNIT_ASSERT (cont != NULL && cont->element != NULL);
		cont = ::width_visit ('b');
		CPPUNIT_ASSERT (cont == NULL);
		cout << "now exit width visit ..." << endl;
	}

	void deep_visit () {
		cout << "now deep visit ..." << endl;
		stack_container *cont = ::deep_visit ('a');
		CPPUNIT_ASSERT (cont != NULL);
		cont = ::deep_visit ('a');
	       	CPPUNIT_ASSERT (cont != NULL);
		cont = ::deep_visit ('c');
		CPPUNIT_ASSERT (cont == NULL);
		cout << "now exit deep visit ..." << endl;
	}

	void free_text_leaf() {
		const char *text = "aba é¸Ÿäºº\naca å“ˆå“ˆ\na å·¥\n";
		::build_text_tree ((jbyte*)text, (jbyte*)text + strlen (text));
		::free_text_leaf (root_tree);
		root_tree = NULL;
	}
	
	void getMoreText () {
		const char *text = "aa é¸Ÿäºº\nac å“ˆå“ˆ\na å·¥\n";
		::build_text_tree ((jbyte*)text, (jbyte*)text + strlen (text));
		stack_container *cont = ::width_visit ('a');
		CPPUNIT_ASSERT (cont != NULL);
		char *str = ::getMoreText (1);
		CPPUNIT_ASSERT (str != NULL);
		std::cout << str << std::endl;
		free (str);
		::free_text_leaf (root_tree);
	}
};

CPPUNIT_TEST_SUITE_REGISTRATION(IMETestFixture);

int main (int argc, char **argv)
{
	CppUnit::TextUi::TestRunner runner;
	CppUnit::TestFactoryRegistry &registry = CppUnit::TestFactoryRegistry::getRegistry();
	runner.addTest (registry.makeTest ());
	runner.run ();
	return 0;
}

