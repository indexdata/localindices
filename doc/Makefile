all: mike-notes.html

%.html: %.md
	rm -f $@
	pandoc --standalone --toc $? > $@
	chmod ugo-w $@

clean:
	rm -f mike-notes.html

