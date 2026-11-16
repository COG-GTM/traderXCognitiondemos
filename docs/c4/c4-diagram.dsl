workspace "FINOS TraderX Sample Application" "An example distributed system in finance." {

    model {
        trader = person "Trader" "Managing Accounts and Creating Trades" "Trader"

        tradingSystem = softwaresystem "Simple Trading System" "Allows employees to create accounts, and execute trades and view positions on these accounts." "Spring Boot Application" {
            ingress = container "Ingress" "Reverse proxy on port 8080 that routes browser traffic to the GUI and to each service by path prefix" "nginx"
            webFrontend = container "Web GUI" "Allows employees to manage accounts and book trades." "Angular (React alternative), TypeScript" "Web Browser"
            peopleService = container "People Service" "Provides user details from a mock directory (people.json)" ".NET 8 / ASP.NET Core"
            accountService = container "Account Service" "Allows employees to manage accounts" "Java and Spring Boot"
            positionService = container "Position Service" "View all trades and positions for an account" "Java and Spring Boot"
            refDataService = container "Reference Data Service" "Provides REST API to securities reference data" "NodeJS (NestJS)"
            tradingService = container "Trading Services" "Validates and accepts new trade orders" "Java and Spring Boot"
            messagebus = container "Trade Feed" "Message bus for streaming updates to trades and positions" "Topic-based Publish-subscribe engine (Socket.IO)"
            tradeProcessor = container "Trade Processor" "Process incoming trade requests, settle, and persist" "Java and Spring Boot"
            database = container "Database" "Stores account, trade, and position state." "H2 Database (TCP server)" "Database"
        }

        trader -> ingress "Opens TraderX in a browser" "HTTP :8080"
        ingress -> webFrontend "Serves the GUI (/)" "HTTP"
        webFrontend -> ingress "Calls services and subscribes to the feed through path prefixes" "REST/JSON/HTTP, WebSocket"

        ingress -> peopleService "Routes /people-service/" "REST/JSON/HTTP"
        ingress -> refDataService "Routes /reference-data/" "REST/JSON/HTTP"
        ingress -> tradingService "Routes /trade-service/" "REST/JSON/HTTP"
        ingress -> accountService "Routes /account-service/" "REST/JSON/HTTP"
        ingress -> positionService "Routes /position-service/" "REST/JSON/HTTP"
        ingress -> messagebus "Routes /socket.io/" "WebSocket/JSON"
        ingress -> database "Routes /db-web/ (H2 console)" "HTTP"

        tradingService -> messagebus "Publishes new trade orders (/trades)" "SocketIO/JSON"
        tradeProcessor -> messagebus "Subscribes to new trades; publishes trade and position updates" "SocketIO/JSON"
        accountService -> database "CRUD operations around accounts." "SQL/JDBC (H2 TCP)"
        positionService -> database "Looks up trades and positions for a given account" "SQL/JDBC (H2 TCP)"
        tradeProcessor -> database "Persists trade state and position state" "SQL/JDBC (H2 TCP)"
        accountService -> peopleService "Validates People IDs when creating/modifying accounts" "REST/JSON/HTTP"
        tradingService -> accountService "Validates accounts when creating trades" "REST/JSON/HTTP"
        tradingService -> refDataService "Validates securities when creating trades" "REST/JSON/HTTP"
    }

    views {

        container  tradingSystem "single-service" {
            include refDataService
            autoLayout
        }

        container  tradingSystem "multiple-services-no-db" {
            include refDataService webFrontend accountService peopleService ingress
            autoLayout
        }

        container  tradingSystem "multiple-services-db-no-messaging" {
            include refDataService webFrontend accountService peopleService ingress database positionService
            autoLayout
        }

        container  tradingSystem "multiple-services-no-async" {
            include refDataService webFrontend accountService peopleService ingress database positionService messagebus
            autoLayout
        }

        container  tradingSystem "full-system" {
            include *
            autoLayout tb 300 250
        }

     styles {
            element "Person" {
                color #ffffff
                background #1168bd
                fontSize 22
                shape Person
            }
            element "Customer" {
                background #08427b
            }
            element "Bank Staff" {
                background #999999
            }
            element "Software System" {
                background #1168bd
                color #ffffff
            }
             element "RecordsMgmt" {
                background #008080
                color #ffffff
            }
            element "External" {
                background #999999
                color #ffffff
            }
            element "Container" {
                background #438dd5
                color #ffffff
            }
            element "Web Browser" {
                shape WebBrowser
            }
            element "Mobile App" {
                shape MobileDeviceLandscape
            }
            element "Content Repository" {
                shape Cylinder
            }
             element "Database" {
                shape Cylinder
            }
            element "Component" {
                background #85bbf0
                color #000000
            }
           }
            }
}
