# Edit the following line to point to the JDK directory
# It must be a JDK, not a JRE !!!
JDK_HOME := /usr/lib/jvm/java-6-sun
#JDK_HOME := /usr/lib/j2sdk1.6-sun

#
# edit anything below at your own risk
#

AVAHI_COMMON_INCLUDE := /usr/include/avahi-common

VER_MAJ=0
VER_MIN=1

ifneq ($(DEBUG),)
DBG := -DDEBUG -Wall
else
OPTIMISATION := -O3
endif


LIB := libavahi4j.so
OBJ := avahi4j_Client.o avahi4j_EntryGroup.o avahi4j_Avahi4JConstants.o thread-watch.o\
		avahi4j_ServiceBrowser.o avahi4j_ServiceResolver.o avahi4j_RecordBrowser.o \
		avahi4j_Address.o

CC := gcc
INCLUDES := -I. -I$(JDK_HOME)/include -I$(JDK_HOME)/include/linux/ -I$(AVAHI_COMMON_INCLUDE)
CFLAGS := $(INCLUDES) $(DBG) $(OPTIMISATION) -fPIC -DVER_MAJ="$(VER_MAJ)" -DVER_MIN="$(VER_MIN)"


LD := gcc
LDFLAGS := -shared  -lavahi-common -lavahi-client

all: version.h $(LIB)

version.h:
	@sh svn-version.sh

$(LIB): $(OBJ)
	$(LD) $(LDFLAGS) $(OBJ) -o $(LIB)

clean:
	rm -f $(OBJ) $(LIB) version.h ./*~

.PHONY: all test clean
