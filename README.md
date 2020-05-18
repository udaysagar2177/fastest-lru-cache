Fastest LRU cache implementation that combines Map and Doubly Linked List functionality
together on every operation - see IntIntLRUCache.java in this repo.

Map and Doubly Linked List are not two different data structures anymore. Data is stored in an
integer array and both map lookups and doubly linked list movements are addressed with respect
to data layout. See the article related to this implementation for more details -
https://medium.com/@udaysagar.2177/fastest-lru-cache-in-java-c22262de42ad
