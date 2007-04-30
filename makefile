# $Id: makefile 252 2006-03-23 23:07:04Z vlads $
#
# Makefile for intelbth.dll
#


SRCDIR=intelbth
OBJDIR=target\obj
OUTDIR=target\obj

DLL_TARGET=intelbth.dll
SRCS=intelbth.cpp

CC=cl.exe

# /W   Warning Level
# /MD  Multithreaded DLL
# /MT  Multithreaded
# /O2  Maximize Speed

CFLAGS=-nologo -I. -I $(JAVA_HOME)\include -I $(JAVA_HOME)\include\win32 \
       /D WIN32 /D _WINDOWS /D NDEBUG /D _USRDLL /D _WINDLL -D _UNICODE  /D UNICODE /D INTELBTH_EXPORTS \
       /O2 /FD /EHsc /MT /W3 /c /Wp64 /Zi /TP \
       $(DEBUG_CFLAGS)

#      /O1 /W3 \


RSC=rc.exe
RSC_PROJ=/l 0x1009 /d "NDEBUG"

LINK=link.exe

LINK_FLAGS=-nologo /MACHINE:X86 /INCREMENTAL:NO /SUBSYSTEM:WINDOWS /OPT:REF /OPT:ICF \
           /DLL /RELEASE /def:"${SRCDIR}\intelbth.def" \
           -LIBPATH:$(OBJDIR) ws2_32.lib irprops.lib kernel32.lib


SRCS_RC=${SRCDIR}\intelbth.rc

OBJS = $(SRCS:.cpp=.obj)
LINK_OBJS=$(OBJS) $(SRCS_RC:.rc=.res)

outdir:
	@if not exist "$(OUTDIR)" mkdir "$(OUTDIR)"

clean:
	@echo clean
	-@erase /Q "$(OBJDIR)\*.obj"
	-@erase /Q "$(OUTDIR)\$(DLL_TARGET)"
	-@erase /Q "*.res"

debug:

install:
	@copy "$(OUTDIR)\$(DLL_TARGET)" "resources\$(DLL_TARGET)"

default: outdir clean dll install

%.obj:
	$(CC) $(CFLAGS) -Fo$(OBJDIR)\$*.obj -c $(SRCDIR)\$*.cpp

%.res:
	rc.exe $(RSC_PROJ) /fo$*.res $*.rc

dll: $(LINK_OBJS)
	$(LINK) $(LINK_FLAGS)  -out:"$(OUTDIR)\$(DLL_TARGET)" $(LINK_OBJS)
