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
//	CPPUNIT_TEST(width_visit);
	//CPPUNIT_TEST(deep_visit);
	//CPPUNIT_TEST(free_text_leaf);
	//CPPUNIT_TEST (getMoreText);
	CPPUNIT_TEST(init_more_text_stack);
	CPPUNIT_TEST(get_more_text);
	CPPUNIT_TEST_SUITE_END();
public:
	void setUp () {
		cout << "setUp exit ..." << endl;
	}

	void tearDown () {
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
		const char *text = "aba 鸟人\naca 哈哈\na 工\n";
		::build_text_tree ((jbyte*)text, (jbyte*)text + strlen (text));
		::free_text_leaf (root_tree);
		root_tree = NULL;
	}
	
	void getMoreText () {
		const char *text = "a a\nb b\nab ab\nac ac\nabc abc\nabd abd\n";
		::build_text_tree ((jbyte*)text, (jbyte*)text + strlen (text));
		stack_container *cont = ::width_visit ('a');
		CPPUNIT_ASSERT (cont != NULL);
		cont = ::width_visit ('b');
		CPPUNIT_ASSERT (cont != NULL);
	}

	void init_more_text_stack () {
		const char *text = "a a\nb b\nab ab\nac ac\nabc abc1 abc2 abc3\nabd abd abd1\nabcd abcd abcd1\n";
		std::cout << "+++++++++++++++++++++++++++++++++++" << std::endl << text
			<< "----------------------------" << std::endl;
		::build_text_tree ((jbyte*)text, (jbyte*)text + strlen (text));
		stack_container *cont = ::width_visit ('a');
		CPPUNIT_ASSERT (cont != NULL);
		cont = ::width_visit ('b');
		CPPUNIT_ASSERT (cont != NULL);

		::init_more_children_stack ();
		CPPUNIT_ASSERT (more_children_stack != NULL);
	}

	void get_more_text () {
			char *buf = NULL;;
		    while ((buf = ::get_more_text (4)) != NULL) {
				std::cout << "get_more_text buf = " << buf << std::endl;
				free (buf);
			}
			CPPUNIT_ASSERT (more_text_stack == NULL);
			CPPUNIT_ASSERT (more_children_stack == NULL);
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

