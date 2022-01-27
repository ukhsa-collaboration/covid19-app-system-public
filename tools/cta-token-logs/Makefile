# make ARGS="pgtcvp5w cc8f0b6z"
run: venv
	$(VENV)/python3 cta-token-logs.py $(ARGS)
.PHONY: run

build:
	$(VENV)/python3 -m mypy cta-token-logs.py
.PHONY: build

clean:
	rm -rf .venv
	rm -rf .mypy_cache
.PHONY: clean

include Makefile.venv
