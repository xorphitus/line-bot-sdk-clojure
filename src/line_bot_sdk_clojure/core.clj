(ns line-bot-sdk-clojure.core
  "Core library for LINE BOT w/ Messaging API"
  (:require [cheshire.core :refer [generate-string parse-string]]
            [clj-http.client :as http-client]
            [pandect.algo.sha256 :refer :all])
  (:import (java.util Base64)
           (java.security MessageDigest)))

(def ^:private line-api-endpoint "https://api.line.me/v2/bot")
(def ^:private line-api-reply-path "/message/reply")
(def ^:private message-limit 5)

(defn reply
  "Reply to a user by using LINE Reply Message API. `message-objects` is vector of maps
  specified by https://devdocs.line.me/ja/#send-message-object and its length is limited
  by LINE API specification."
  [to-user-id reply-token message-objects line-channel-token]
  (let [body (generate-string {:to to-user-id
                               :replyToken reply-token
                               :messages (take message-limit message-objects)})]
    (http-client/post (str line-api-endpoint line-api-reply-path)
     {:body body
      :headers {"Authorization" (str "Bearer " line-channel-token)}
      :content-type :json})))

(defn validate-signature
  "Validate a request from LINE, whether it is sent from actual LINE webhook."
  [content signature line-channel-secret]
  (let [hash (sha256-hmac-bytes content line-channel-secret)
        decoded-signature (.. Base64 getDecoder (decode signature))]
    (MessageDigest/isEqual hash decoded-signature)))

(defn- compile-event [name args handler]
  (let [arg (first args)]
    {name `(fn[~arg] ~handler)}))

(defmacro MESSAGE
  "Generate a Message event handler dispatcher."
  [arg handler]
  (compile-event "message" arg handler))

(defmacro FOLLOW
  "Generate a Follow event handler dispatcher."
  [arg handler]
  (compile-event "follow" arg handler))

(defmacro UNFOLLOW
  "Generate a Unfollow event handler dispatcher."
  [arg handler]
  (compile-event "unfollow" arg handler))

(defmacro JOIN
  "Generate a Join event handler dispatcher."
  [arg handler]
  (compile-event "join" arg handler))

(defmacro LEAVE
  "Generate a Leave event handler dispatcher."
  [arg handler]
  (compile-event "leave" arg handler))

(defmacro POSTBACK
  "Generate a Postback event handler dispatcher."
  [arg handler]
  (compile-event "postback" arg handler))

(defmacro BEACON
  "Generate a Beacon event handler dispatcher."
  [arg handler]
  (compile-event "beacon" arg handler))

(defmacro ELSE
  "Generate an event handler dispatcher which is used when no handlers are defined
  to a event."
  [arg handler]
  (compile-event :else arg handler))

(defmacro deflineevents
  "Define event dispatchings."
  [name & forms]
  `(defn ~name [request-body#]
     (let [handler-map# (apply merge ~@forms)
           events# (:events (parse-string request-body# true))]
       (map (fn [event#]
              (let [ev-type# (:type event#)]
                (if-let [handler# (get handler-map# ev-type#)]
                  (if (or (not= ev-type# "message") (fn? handler#))
                    (handler# event#)
                    (let [sub-type# (get-in event# [:message :type])
                          sub-events# handler#]
                      (if-let [sub-handler# (get sub-events# sub-type#)]
                        (sub-handler# event#)
                        ((:else sub-events#) event#))))
                  ((:else handler-map#) event#))))
            events#))))
