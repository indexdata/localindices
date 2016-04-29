OUT = harvester-api.html dev-notes.html

all: $(OUT)

%.html: %.raml
	rm -f $@
	raml2html $? > $@
	chmod 600 $@

%.html: %.md
	rm -f $@
	pandoc -c style.css --standalone --toc $? > $@
	chmod ugo-w $@

clean:
	rm -f $(OUT)
