(ns repo-tracker.core
  (:require
    [day8.re-frame.http-fx]
    [reagent.dom :as rdom]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [repo-tracker.ajax :as ajax]
    [repo-tracker.events :as events]
    [markdown.core :refer [md->html]]))

(defn navbar []
  [:nav.navbar.is-info>div.container
   [:div.navbar-brand
    [:a.navbar-item {:href "/" :style {:font-weight :bold}} "repo-tracker"]
    (when-let [user @(rf/subscribe [::events/user])]
      [:a.navbar-item {:style {:font-weight :bold}} (:user-id user)])]])

(defn input
  ([label action] (input label nil action))
  ([label placeholder action]
   (r/with-let [value (r/atom nil)]
     [:div.field.has-addons
      [:div.control>input.input
       {:type        :text
        :placeholder placeholder
        :on-change   #(reset! value (-> % .-target .-value))}]
      [:div.control>button.button
       {:disabled (empty? @value)
        :on-click #(rf/dispatch [action @value])}
       label]])))

(defn format-date [date-str]
  (.toLocaleDateString (js/Date. (js/Date.parse date-str))
                       "en-us"
                       #js{:weekday "short"
                           :year    "numeric"
                           :month   "short"
                           :day     "numeric"
                           :hour    "numeric"
                           :minute  "numeric"}))

(defn repo-button [{:keys [id seen?]} [{:keys [created_at]}]]
  (let [highlight? (not seen?)]
    [:div
     {:on-click #(rf/dispatch [::events/set-selected-repo id])
      :style    {:cursor :pointer}}
     [:h4 id]
     [:div.columns
      [:div.column
       [:div
        [:div (format-date created_at)]
        (when highlight?
          [:button.button.is-primary
           {:on-click #(rf/dispatch [::events/mark-repo-as-seen id])}
           "New!"])]]
      [:div.column>button.button
       {:on-click #(rf/dispatch [::events/remove-repo id])}
       "X"]]]))

(defn release-details [{:keys [name draft prerelease url body]}]
  [:div
   [:h4 name]
   (when draft [:span "this is a draft release"])
   (when prerelease [:span "this is a draft prerelease"])
   [:a {:href url}]
   [:p {:dangerouslySetInnerHTML {:__html (md->html body)}}]])

(defn repos-view []
  (if @(rf/subscribe [::events/loading?])
    [:div.notification.is-info.is-light
     "loading data"]
    [:div
     [:div.columns
      [:div.column.is-one-quarter [input "add repository" "org/repository" ::events/add-repo]]
      [::div.column.is-one-quarter>button.button
       {:on-click #(rf/dispatch [::events/refresh-repos])}
       "refresh repositories"]]
     [:div.columns
      [:div.column.is-one-quarter>ul
       (for [[repo-id {meta :meta data :data}] @(rf/subscribe [::events/repos])]
         ^{:key repo-id}
         [:li [repo-button meta data]])]
      [:div.column>ul
       (if-let [repo @(rf/subscribe [::events/selected-repo])]
         (for [release repo]
           ^{:key (str (:id release))}
           [:li [release-details release]])
         [:span "select a repository to view releases"])]]]))

(defn login-view []
  [:div
   [:label "user "]
   [input "login" ::events/login]])

(defn error-component []
  (when-let [error @(rf/subscribe [:common/error])]
    [:div.notification.is-danger
     [:button.delete {:on-click #(rf/dispatch [:common/set-error nil])}]
     (str error)]))

(defn page []
  [:div
   [navbar]
   [:section.section>div.container>div.content
    [error-component]
    (if @(rf/subscribe [::events/user])
      [repos-view]
      [login-view])]])

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (ajax/load-interceptors!)
  (mount-components))
