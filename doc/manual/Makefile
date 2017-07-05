SOURCE = 00-metadata.md 01-overview.md 02-using.md 03-appendices.md

all: harvester-manual.html harvester-manual.pdf

harvester-manual.html: 
	pandoc -o harvester-manual.html --toc $(SOURCE)

harvester-manual.pdf: 
	pandoc -V geometry:margin=50pt -o harvester-manual.pdf --toc --include-before-body=00-titlepage.tex $(SOURCE)

clean:
	rm -f harvester-manual.html harvester-manual.pdf

