# this is a Makefile for cppunit test code
JDK_HOME = /cygdrive/c/Program\ Files/Java/jdk1.6.0_25/include
#INCLUDES_DIR = -I/usr/include/boost
INCLUDES_DIR += -I$(JDK_HOME)
LIBS_DIR=./
CXX_FILES = $(wildcard *.cpp)
CXX_FLAG+=-Wall -g
OBJECTS = $(patsubst %.cpp, %.o, $(CXX_FILES))
LIBS += -lcppunit
CC = c++
TARGET = main

all:$(TARGET)

$(TARGET):$(OBJECTS)
	$(CC) $(OBJECTS) $(LIBS) -L$(LIBS_DIR) -o $@
		   

$(OBJECTS):$(CXX_FILES)
	$(CC) $(CXX_FLAG) $(INCLUDES_DIR) $< -c -o $@

.PHONY: clean

clean:
	@rm *.o
	@rm $(TARGET)

