(ns smallworld.main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [PIXI :as p]
            [goog.dom :as dom]
            [cljs.core.async :as a :refer [<! chan]]
            [rum.core :as r]
            [adzerk.boot-reload.reload :as rl]))

(set! p/SCALE_MODES.DEFAULT p/SCALE_MODES.NEAREST)

(enable-console-print!)

(defn wait-for-assets [loader assets]
  (let [loaded (chan)]
    (go (doseq [asset assets]
          (.add loader asset))
        (.load loader (fn [loader resources] (a/put! loaded true)))
        (<! loaded))))

(defn sprite!
  ([texture]
   (sprite! texture 0 0))
  ([texture x y]
   (let [sprite   (p/Sprite. texture)]

     (set! (.. sprite -position -x) x)
     (set! (.. sprite -position -y) y)

     (set! (.. sprite -width) 256)
     (set! (.. sprite -height) 256)

     sprite)))

(defn cache-buster! []
  (rand-int 1000000))


(defn init-pixi!
  [{:keys [width height]}]
  (let [images   {:blue (str "images/first.png?" (cache-buster!))}
        stage    (p/Container.)
        renderer (p/autoDetectRenderer width height)
        loader   (p/loaders.Loader.)]

    (go
      (<! (wait-for-assets loader (vals images)))

      (let [texture       (.-texture (aget (.-resources loader) (:blue images)))
            textures [;; mountains
                      (p/Texture. texture (p/Rectangle. 0 0 32 32))
                      (p/Texture. texture (p/Rectangle. 32 0 32 32))
                      ;; pyramids/plains
                      (p/Texture. texture (p/Rectangle. 0 32 32 32))
                      (p/Texture. texture (p/Rectangle. 32 32 32 32))
                      ;; sky
                      (p/Texture. texture (p/Rectangle. 0 96 32 32))
                      (p/Texture. texture (p/Rectangle. 32 96 32 32))
                      ;; sky
                      (p/Texture. texture (p/Rectangle. 0 128 32 32))
                      (p/Texture. texture (p/Rectangle. 32 128 32 32))
                      ]
            offset 2]

        ;; sky
        (doseq [x (range 5)]
          (.addChild stage (sprite! (textures (+ 4 (rand-int 2))) (* x 256) 0)))

        (doseq [x (range 5)]
          (.addChild stage (sprite! (textures (+ 6 (rand-int 2))) (* x 256) 256)))

        ;; mountains
        (doseq [x (range 5)]
          (.addChild stage (sprite! (textures (rand-int 2)) (* x 256) (* offset 256))))

        ;; pyramids
        (doseq [x (range 5)]
          (.addChild stage (sprite! (textures (+ 2 (rand-int 2))) (* x 256) (+ (* offset 256) 256))))
)

      {:images   images
       :stage    stage
       :renderer renderer
       :loader   loader
       :destroy  (fn []
                   (doseq [image (js/Object.keys p/utils.BaseTextureCache)]
                     (println :image image)

                     (if-let [texture (aget p/utils.BaseTextureCache image)]
                       (do
                         (.log js/console texture)
                         (.destroy texture)
                         (goog.object.remove p/utils.BaseTextureCache image))))

                   (doseq [image (js/Object.keys p/utils.TextureCache)]
                     (println :image image)

                     (if-let [texture (aget p/utils.TextureCache image)]
                       (do
                         (.log js/console texture)
                         (.destroy texture)
                         (goog.object.remove p/utils.TextureCache image))))

                   (.remove (.-view renderer))

                   (.destroy stage)
                   (.destroy renderer))})))

(defn attach!
  [el {:keys [renderer]} {:keys [width height]}]

  (println :attach! width height)
  (.appendChild el (.-view renderer)))

(defn render!
  [{:keys [images stage renderer loader]}]
  (println :render!!)
  (.render renderer stage))

(def pixi-canvas
  {:did-mount (fn [state]
                (let [comp            (:rum/react-component state)
                      [system config] (:rum/args state)
                      dom-node        (js/ReactDOM.findDOMNode comp)]

                  (attach! dom-node system config)
                  (render! system)

                  state))

   :did-update (fn [state]
                 (let [comp            (:rum/react-component state)
                       [system config] (:rum/args state)
                       dom-node        (js/ReactDOM.findDOMNode comp)]

                   (attach! dom-node system config)

                   (render! system)
                   state))})

(r/defc canvas < pixi-canvas [system config]
  [:div])

(r/defc root < r/reactive [system config]
  (canvas (r/react system) config))

(def system (atom {}))

(defn reset-pixi! [config]
  (go
    (let [{:keys [destroy]} @system]
      (when destroy
        (destroy)))

    (let [sys (<! (init-pixi! config))]
      (reset! system sys)
      (println :reset))))

(def config {:width (* 256 5) :height (* 256 4)})

(defn main []
  (go
    (<! (reset-pixi! config))

    (r/mount (root system config) (dom/getElement "root"))
    (println "Hello, ludumdare!")))

(defmethod adzerk.boot-reload.client/handle :reload
  [{:keys [files]} opts]
  (println :files files)

  (go (<! (reset-pixi! config))
      (rl/reload files opts)))

;; override :visual as workaround for gimp png exports not going to browser
(defmethod adzerk.boot-reload.client/handle :visual
  [{:keys [files]} opts]
  (println :files files)

  (go (<! (reset-pixi! config))))
