(ns smallworld.main
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [PIXI :as p]
            [goog.dom :as dom]
            [cljs.core.async :as a :refer [<! chan]]
            [rum.core :as r]))

(enable-console-print!)

(defn wait-for-assets [assets]

  (let [loaded         (chan)
        missing-assets (filter (fn [asset]
                                 (not (aget (.-resources p/loader) asset)))
                               assets)]

    (doseq [asset missing-assets]
      (.add p/loader asset))

    (go (cond (not (empty? missing-assets))
              (.load p/loader (fn [loader resources] (a/put! loaded true)))

              :else
              (a/put! loaded true))

        (<! loaded)

        (.-resources p/loader))))

(defn sprite! [texture]
  (let [sprite   (p/Sprite. texture)]

    ;; (set! (.. sprite -position -x) 0)
    ;; (set! (.. sprite -position -y) 0)

    sprite))

(defn init-pixi! [el]
  (let [stage     (p/Container.)
        renderer  (p/autoDetectRenderer 200 200)]

    (go (let [images    {:blue "images/first.png"}

              resources (<! (wait-for-assets (vals images)))

              sprite    (sprite! (.-texture (aget resources (:blue images))))]

          (.appendChild el (.-view renderer))

          (.addChild stage sprite)

          (.render renderer stage)))

    {:renderer renderer
     :stage    stage}))

(def pixi-canvas
  {:did-mount (fn [state]
                (let [comp     (:rum/react-component state)
                      dom-node (js/ReactDOM.findDOMNode comp)]

                  (assoc state ::pixi (init-pixi! dom-node))))

   :will-unmount (fn [state]
                   (println (-> state ::pixi))
                   (.destroy (-> state ::pixi :stage))
                   (.destroy (-> state ::pixi :renderer))

                   (dissoc state ::pixi))})

(r/defc canvas < pixi-canvas []
  [:div])

(defn main []
  (r/mount (canvas) (dom/getElement "root"))
  (println "Hello, ludumdare!"))
