HOW DO I DO REST? SEE STORM MAILING LIST

thanmarz    
07/05/2012


The best way is through an intermediate broker, like Kestrel or Kafka. Spouts for these are available from the following locations:

1. KestrelSpout: https://github.com/nathanmarz/storm-kestrel
2. KafkaSpout: https://github.com/nathanmarz/storm-contrib/tree/master/storm-kafka

There are also spout implementations available for many other queuing brokers: https://github.com/nathanmarz/storm/wiki/Spout-implementations
- show quoted text -
-- 
Twitter: @nathanmarz
http://nathanmarz.com


