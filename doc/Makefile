OUT = harvester-api.html mike-notes.html

all: $(OUT)

%.html: %.raml
	rm -f $@
	raml2html $? > $@
	chmod 600 $@

%.html: %.md
	rm -f $@
	pandoc --standalone --toc $? > $@
	chmod ugo-w $@

clean:
	rm -f $(OUT)
