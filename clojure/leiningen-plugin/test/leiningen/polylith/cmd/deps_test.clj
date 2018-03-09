(ns leiningen.polylith.cmd.deps-test
  (:require [clojure.test :refer :all]
            [leiningen.polylith.cmd.test-helper :as helper]
            [leiningen.polylith.file :as file]
            [leiningen.polylith :as polylith]))

(use-fixtures :each helper/test-setup-and-tear-down)

(def dependencies {'authentication ['common.interface/check-password
                                    'common.interface/db
                                    'common.interface/pull-query]
                   'authorization ['common.interface/db
                                   'common.query.interface/admin?
                                   'common.query.interface/manager?
                                   'common.query.interface/staff?
                                   'user.interface/find-user],
                   'backend ['authentication.interface/create-auth-token
                             'authentication.interface/invalidate-refresh-token
                             'authentication.interface/refresh-auth-token
                             'authorization.interface/has-rights?
                             'common.interface/apply-schema-changes-to-db!
                             'common.interface/init-logging
                             'common.interface/send-message
                             'common.interface/unsign-token
                             'common.interface.environment/allowed-origins
                             'db-delta.interface/apply-deltas-to-db!
                             'user.interface/create-user!
                             'user.interface/delete-user!]})

(deftest polylith-deps--interface-deps--print-interface-dependencies
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                leiningen.polylith.cmd.deps/all-dependencies (fn [_] dependencies)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company" "my/company")
          output (with-out-str
                   (polylith/polylith project "deps"))]
      (is (= (str
                "authentication:\n"
                "  common\n"
                "authorization:\n"
                "  common\n"
                "  user\n"
                "backend:\n"
                "  authentication\n"
                "  authorization\n"
                "  common\n"
                "  db-delta\n"
                "  user\n")
             output)))))

(deftest polylith-deps--interface-deps--print-function-dependencies
  (with-redefs [file/current-path (fn [] @helper/root-dir)
                leiningen.polylith.cmd.deps/all-dependencies (fn [_] dependencies)]
    (let [ws-dir (str @helper/root-dir "/ws1")
          project (helper/settings ws-dir "my.company" "my/company")
          output (with-out-str
                   (polylith/polylith project "deps" "f"))]
      (is (= (str
               "authentication:\n"
               "  common.interface/check-password\n"
               "  common.interface/db\n"
               "  common.interface/pull-query\n"
               "authorization:\n"
               "  common.interface/db\n"
               "  common.query.interface/admin?\n"
               "  common.query.interface/manager?\n"
               "  common.query.interface/staff?\n"
               "  user.interface/find-user\n"
               "backend:\n"
               "  authentication.interface/create-auth-token\n"
               "  authentication.interface/invalidate-refresh-token\n"
               "  authentication.interface/refresh-auth-token\n"
               "  authorization.interface/has-rights?\n"
               "  common.interface/apply-schema-changes-to-db!\n"
               "  common.interface/init-logging\n"
               "  common.interface/send-message\n"
               "  common.interface/unsign-token\n"
               "  common.interface.environment/allowed-origins\n"
               "  db-delta.interface/apply-deltas-to-db!\n"
               "  user.interface/create-user!\n"
               "  user.interface/delete-user!\n")
             output)))))
