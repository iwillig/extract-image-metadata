rebel:
	clojure -M:dev:rebel

outdated:
	clojure -M:dev:outdated

test:
	clojure -M:dev:tests

extract.jar: src/**/*.clj
	clojure -X:uberjar :jar extract.jar :main-class extract-image-metadata.main

run:
	java -jar extract.jar

clean:
	-rm extract.jar
	-rm extract
	-rm extract.build_artifacts.txt
	-rm -rf classes

native:
	clojure -M:native-image

.PHONY: run clean rebel outdated test
