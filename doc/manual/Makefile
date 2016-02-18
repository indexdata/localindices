SOURCE = 00-metadata.md 01-overview.md 02-using.md

all: manual.html manual.pdf

manual.html: 
	pandoc -o manual.html --toc $(SOURCE)

manual.pdf: 
	pandoc -V geometry:margin=50pt -o manual.pdf --toc --include-before-body=00-titlepage.tex $(SOURCE)

clean:
	rm -f manual.html manual.pdf

