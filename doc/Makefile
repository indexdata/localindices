OUT = harvester-api.html

all: $(OUT)

%.html: %.raml
	rm -f $@
	raml2html $? > $@
	chmod 600 $@

clean:
	rm -f $(OUT)

