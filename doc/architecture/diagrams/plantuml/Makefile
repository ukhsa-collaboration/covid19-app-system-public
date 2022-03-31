DIAGRAMS_SRC := $(wildcard *.plantuml)
DIAGRAMS_PNG := $(addsuffix .png, $(basename $(DIAGRAMS_SRC)))
DIAGRAMS_SVG := $(addsuffix .svg, $(basename $(DIAGRAMS_SRC)))
LOCAL_DATE := $(shell date "+%Y-%m-%d")

png: build $(DIAGRAMS_PNG)
svg: build $(DIAGRAMS_SVG)

%.png: %.plantuml
	cat $< | docker run --rm -i nhsx/plantuml -tpng > ../img/$*-${LOCAL_DATE}.png

%.svg: %.plantuml
	cat $< | docker run --rm -i nhsx/plantuml > ../img/$*-${LOCAL_DATE}.png

build:
	docker build --build-arg plantuml_version=1.2022.2 -t nhsx/plantuml:latest .

.PHONY: build png svg postbuild
