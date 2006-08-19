TIDY = tidy
MARKDOWN = markdown
FILES = index.html

all: $(FILES)

%.xhtml: %.txt
	$(MARKDOWN) $^ > $@

%.html: %.xhtml
	mv -f $^ $@
	-$(TIDY) -q -m -asxhtml $@

clean:
	rm -f *.html *.xhtml
