all:
	pandoc -V geometry:margin=50pt -o manual.pdf --toc --include-before-body=00-titlepage.tex \
	  00-metadata.md 01-overview.md \
	  02-using.md

clean:
	rm manual.pdf
