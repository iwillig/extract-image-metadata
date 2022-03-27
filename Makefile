rebel:
	clojure -M:dev:rebel

outdated:
	clojure -M:dev:outdated

test:
	clojure -M:dev:tests

extract-image-metadata.jar: src/**/*.clj
	clojure -X:uberjar :jar extract-image-metadata.jar

run:
	java -cp extract-image-metadata.jar clojure.main -m extract-image-metadata.main

clean:
	rm extract-image-metadata.jar

.PHONY: run clean rebel outdated test
