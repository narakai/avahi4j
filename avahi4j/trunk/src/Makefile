# Edit the following line to point to the JDK directory
# It must be a JDK, not a JRE !!!
JDK_HOME := /usr/lib/jvm/java-6-sun
#JDK_HOME := /usr/lib/j2sdk1.6-sun
AVAHI_COMMON_INCLUDE := /usr/include/avahi-common


#
# edit anything below at your own risk
#


ifneq ($(DEBUG),)
DBG := -DDEBUG -Wall
else
OPTIMISATION := -O3
endif


LIB := libavahi4j.so
OBJ := avahi4j_Client.o avahi4j_EntryGroup.o avahi4j_Avahi4JConstants.o thread-watch.o

CC := gcc
INCLUDES := -I. -I$(JDK_HOME)/include -I$(JDK_HOME)/include/linux/ -I$(AVAHI_COMMON_INCLUDE)
CFLAGS := $(INCLUDES) $(DBG) $(OPTIMISATION) -fPIC

LD := gcc
LDFLAGS := -shared  -lavahi-common -lavahi-client

all: $(LIB)

$(LIB): $(OBJ)
	$(LD) $(LDFLAGS) $(OBJ) -o $(LIB)

clean:
	rm -f $(OBJ) $(LIB) ./*~

.PHONY: all test clean
