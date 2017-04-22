(ns smallworld.styles
  (:require [garden.def :refer [defstylesheet defstyles]]
            [garden.units :refer [px]]))

(defstyles screen
  [:body :html
   {:background-color "#24a3d6"
    :color "#ffffff"
    :font-family "'Roboto', sans-serif"
    :height "100%"}]
  [:canvas
   {:border "1px solid black"}])
