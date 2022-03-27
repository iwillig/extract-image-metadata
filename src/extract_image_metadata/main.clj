(ns extract-image-metadata.main
  (:require
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [clojure.tools.cli :as cli])
  (:import
   (com.drew.metadata.exif GpsDirectory)
   (com.drew.imaging ImageMetadataReader)
   (javax.imageio ImageIO))
  (:gen-class))

(defn display-attrs
  [_node attributes]
  (let [count (.getLength attributes)]
    (into {}
           (mapv (fn [x-index]
                   (let [x (.item attributes x-index)]
                     [(str (.getNodeName x))
                      (.getNodeValue x)]))
                 (range 0 count)))))

(defn display-node
  [node]
  [(.getNodeName node)
   (when-let [child (.getFirstChild node)]
     (display-node child))
   (display-attrs node (.getAttributes node))])

(defn display-recur
  [node]
  (loop [node node property {}]
    (if-let [child (.getFirstChild node)]
      (let [node-name (str (.getNodeName node))]
        (recur child (assoc property node-name (display-node child))))
      property)))

(defn list-files
  [path]
  (.listFiles (java.io.File. path)))

(comment
  (let [stream (ImageIO/createImageInputStream file)
        readers (ImageIO/getImageReaders stream)]
    [(.getName file)
     (let [things (atom [])]
       (while (.hasNext readers)
         (let [reader (.next readers)]
           (.setInput reader stream true)
           (let [metadata (.getImageMetadata reader 0)]
             (doall
              (for [name (seq
                          (.getMetadataFormatNames metadata))]
                (swap! things conj [(.getName file)
                                    (display-recur (.getAsTree metadata name))]))))))
       @things)])

  (into {}
        (for [file (list-files)]
          [(.getName file)
           (select-keys
            (into {}
                  (for [dir (.getDirectories (ImageMetadataReader/readMetadata file))]
                    [(.getName dir)
                     (into {}
                           (for [tag (.getTags dir)]
                             [(.getTagName tag)
                              (.getDescription tag)]))]))
            ["GPS"])])))


(def cli-options
  [["-f" "--folder FOLDER" :id :folder]
   ["-o" "--out-file FILE" :id :out-file]])

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
            (let [gps-info (pull-gps-info-from-file file)]
              (when (some? gps-info)
                (conj gps-info (.getName file)))))
          files)))

(def header (list "name" "latitude" "longitude"))

(defn -main [& args]
  (let [options (cli/parse-opts args cli-options)]
    (when-not (seq (:errors options))

      (let [folder (get-in options [:options :folder])
            out-file (get-in options [:options :out-file])
            gps-info (pull-gps-info-from-folder folder)]
        (with-open [writer (io/writer out-file)]
          (csv/write-csv writer
                         (doall (conj gps-info header))))))))

(comment

  (-main "--folder" "images"
         "--out-file" "test.csv")


  )
