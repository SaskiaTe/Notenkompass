# Hier erstellt ihr alle Befehle damit die komplette Entwicklungsumgebung, installiert und gestartet werden kann
# Die einzelnen Schrite um die Anwedungen zu starten findet ihr im README.md

test:
	cd backend && mvn test

clean:
	cd backend && mvn clean package

.PHONY: docker

docker:
	docker build -t grade-compass-backend:latest backend


.PHONY: docker-clean

docker-clean:
	docker rm -f notenkompass-backend || true
	docker compose -f backend/compose.yaml down --rmi all
	docker rmi grade-compass-backend:latest || true
