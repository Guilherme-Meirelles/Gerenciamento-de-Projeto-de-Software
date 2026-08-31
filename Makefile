MVN := ./mvnw

.PHONY: help build run test clean package install verify format

help:
	@echo "Alvos disponiveis:"
	@echo "  make run       - Executa a aplicacao (spring-boot:run)"
	@echo "  make build     - Compila o projeto"
	@echo "  make package   - Gera o pacote (jar) em target/"
	@echo "  make test      - Executa os testes"
	@echo "  make verify    - Roda o ciclo completo de verificacao"
	@echo "  make install   - Instala o artefato no repositorio local"
	@echo "  make clean     - Remove os artefatos de build"

run:
	$(MVN) spring-boot:run

build:
	$(MVN) compile

package:
	$(MVN) package

test:
	$(MVN) test

verify:
	$(MVN) verify

install:
	$(MVN) install

clean:
	$(MVN) clean
