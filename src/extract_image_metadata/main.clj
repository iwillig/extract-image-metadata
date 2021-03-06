(ns extract-image-metadata.main
  (:require
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [clojure.tools.cli :as cli]
   [clojure.string :as string])
  (:import
   (com.drew.metadata.exif GpsDirectory)
   (com.drew.imaging ImageMetadataReader))
  (:gen-class))

(defn list-files
  [path]
  (.listFiles (java.io.File. path)))

(def cli-options
  [["-f" "--folder FOLDER" :id :folder]
   ["-o" "--out-file FILE" :id :out-file]
   ["-h" "--help"]])

;; metadata.getFirstDirectoryOfType();
;; ;; metadata
;; GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
;; ;;
;;        GeoLocation geoLocation = gpsDirectory.getGeoLocation();
;;        assertEquals(54.989666666666665, geoLocation.getLatitude(), 0.001);
;;        assertEquals(-1.9141666666666666, geoLocation.getLongitude(), 0.001);

;; GpsDirectory/TAG_DATETIME
(defn pull-gps-info-from-file
  [file]
  (let [meta (ImageMetadataReader/readMetadata file)]
    (some
      (fn [x] (some? x) x)
      (for [dir (.getDirectories meta)
            :when (instance? GpsDirectory dir)
            :let [location (.getGeoLocation dir)]]
        (list
         (.getLatitude location)
         (.getLongitude location))))))

;; data structure is
;; (list file-name, latitude, longitude)
(defn pull-gps-info-from-folder
  [folder]
  (let [files (list-files folder)]

    (keep (fn [file]
            (when (.isFile file)
              (let [gps-info (pull-gps-info-from-file file)]
                (when (some? gps-info)
                  (conj gps-info (.getName file))))))
          files)))

(def header (list "name" "latitude" "longitude"))

(defn write-csv
  [{:keys [folder out-file]}]
  (let [gps-info (pull-gps-info-from-folder folder)]
    (with-open [writer (io/writer out-file)]
      (csv/write-csv writer
                     (doall (conj gps-info header))))
    "success"))

(defn usage [options-summary]
  (->> ["Tool for extracting gsp location information from image files"
        "Usage: extract-gps-info [options]"
        "Uses the  com.drewnoakes/metadata-extractor java library for information"
        "Options:"
        options-summary
        ""]
       (string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status)
  #_(println `(System/exit ~status)))

(defn -main [& args]
  (let [{:keys [options errors summary]} (cli/parse-opts args cli-options)]

    (cond
      (true? (:help options))
      (exit 0 (usage summary))
      (seq errors)
      (exit 1 (error-msg errors))
      :else
      (exit 0 (write-csv options)))))

(comment

  (-main "--folder" "images"
         "--out-file" "test.csv")

  (-main "--help")


  )
