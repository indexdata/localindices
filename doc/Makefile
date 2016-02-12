all: mike-notes.html

%.html: %.md
	rm -f $@
	pandoc $? > $@
	chmod ugo-w $@

clean:
	rm -f mike-notes.html

