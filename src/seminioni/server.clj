(ns seminioni.server
  (:require 
    [immutant.web :as web]
    [compojure.core :as cj]
    [compojure.route :as cjr]
    [clojure.java.io :as io]
    [clojure.edn :as edn]
    [rum.core :as rum])
  (:import
    [org.joda.time DateTime]
    [org.joda.time.format DateTimeFormat]))

(def styles (slurp (io/resource "styles.css")))

(def date-formatter (DateTimeFormat/forPattern "dd.MM.YYYY"))

(defn render-date [inst]
  (.print date-formatter (DateTime. inst)))

(defn post-ids []
  (for [name (seq (.list (io/file "posts")))
        :let [child (io/file "posts" name)]
        :when (.isDirectory child)]
    name))

(rum/defc post [post]
  [:.post
   [:.post_sidebar
    [:img.avatar {:src (str (:author post) ".jpg")}]]
  [:div
    (for [name (:pictures post)]  
      [:img {:src (str "/post/" (:id post) "/" name) }])
   [:p [:span.author (:author post) ": "] (:body post)]
   [:p.meta (render-date (:created post)) "//" [:a {:href (str "/post/" (:id post))} "Ссылка"]]]])

(rum/defc page [title & children]
  [:doctype {:html ""}]
  [:html
   [:head
    [:meta { :http-equiv "Content-Type" :content "text/html; charset=UTF-8" }]
    [:title title]
    [:meta { :name "viewport" :content "width=device-width, initial-scale=1.0"}]
    [:style { :dangerouslySetInnerHTML { :__html styles }} ]]
   [:body
    [:header
     [:h1 "Молчания бобчат"]
     [:p#site_subtitle "Это боб"]]
    children]])

(rum/defc index [post-ids]
  (page "Молчание бобчат" 
    (for [post-id post-ids
          :let [path (str "posts/" post-id "/post.edn")
                p    (-> (io/file path)
                         (slurp)
                         (edn/read-string))]]
      (post p))))

(defn with-headers [handler headers]
  (fn [request]
    (some-> (handler request)
      (update :headers merge headers))))

(defn render-html [component]
  (str (rum/render-static-markup component)))

(cj/defroutes routes
  (cjr/resources "/" { :root "public" })
  
  (cj/GET "/" []
    { :body (render-html (index (post-ids)))})
  
  (cj/GET "/post/:id/:img" [id img]
    (ring.util.response/file-response (str "posts/" id "/" img)))
  
  (fn [req]
    { :status 404
      :body "404 Not found" }))

(def app 
  (-> routes 
    (with-headers { "Content-Type" "text/html; charset=utf-8" 
                    "Cache-Control" "no-cache"
                    "Expires" "-1" })))

(defn -main [& args]
  (let [args-map (apply array-map args)
        port-str (or (get args-map "-p")
                     (get args-map "--port")
                     "8080")]
    (println "Staring web server on port " port-str)
    (web/run #'app { :port (Integer/parseInt port-str) })))

(comment 
  (def server (-main "--port" "8000"))
  (web/stop server))