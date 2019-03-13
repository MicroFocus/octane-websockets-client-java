### Changelog

##### 2.0
- moving the the generic Octane configuration reader, removing all custom code of reading and parsing configurations

##### 1.1.0
- fixed the case when `setup.xml` contains an empty `init string` entry

##### 1.0.4
- added SpotBugs to the `pom` (`quality` profile should be used to run `spotbugs:check` or alike)
- changed initialization flow to init ES clients eargerly on start-up
- added verification logic to the TransportClient (in order it trigger connection, which should start effectively sniffing, which should help to cope with AWS hosts switches)

##### 1.0.3
- updating `octane-component-parent` version

##### 1.0.2
- changed behavior of running side-by-side with Octane when Octane's ES is embedded (rare case, CI as of now and probably will also be changed), now ES connector will attempt to create remote configuration (previously was running embedded node) that connects to localhost:port

##### 1.0.1
- upgrade flow support
- updating ES to 5.6.0 version
- adding ability to resolve configuration from `Octane's` `setup.xml` (for side-by-side run scenario)

##### 1.0.0
- initial version
- Elasticsearch configuration supplied by `properties`, `xml`, `yaml/yml` resources supported
- Elasticsearch configuration resource location customizable (via system property `es.config.location`)
- multiple data sources supported