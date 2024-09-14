# Minimalistic mongodb framework

Mongoman is an easy to use framework that maps your java classes to mongodb objects

# Installation

Download dist/mongoman.jar and include it in your libraries (or compile your own version)

Must also include mongo-java-driver in your project

# Usage

#### Loading and storing objects into mongodb
- Implement a class that **extends** mongoman.Base
- Each class will represent a unique collection in the database
- Collection name must be passed via @Kind annotation with the class
- Only **non-static public** fields get stored
- **non-static final public** fields are used as keys to load and store the objects and are indexed to prevent duplicates.
- Use load() and store() to load and save the object
- Your class must implement a 0-argument constructor to allow Mongoman to instantiate objects when loading data from the database.

##### Example

```
@Kind("car")
class Car extends Base {
    public int myInteger;               // stored in db
    public double[] myDoubleArray;      // stored in db
   
    public final String myId;   // saved in db, also unique across collection
    private double priv;        // this will not get stored in db

    public Car(...) {
        myId = ....
        .....
    }
    
    // must implement a 0-argument constructor
    public Car() {
        ....
    }
        
}
```

```
Datastore store = new Datastore(....);
store.setDefaultService(store);

Car c = new Car(..);
c.load();
c.myInteger = 20;
c.save();
```

#### Supported types
Not all fields are automatically serialized into mongodb, the following are the ones guaranteed to work

- **Primitive types**: `int`, `byte`, `float`, `double`, etc.
- **String**
- **Enums**: Java `enum` types are fully supported, and enum values are stored as strings
- **Dates**: java.util.Date
- **Nested `Base` classes**: Any class that extends `mongoman.Base`

These types can also be used within:

- **Primitive arrays**: Arrays of primitives such as `int[]`, `byte[]`, `float[]`, etc.
- **Arrays**: Arrays of any type mentioned above.
- **Collections**:
  - `java.util.List`: Lists of any type mentioned above.
  - `java.util.Set`: Sets of any type mentioned above.
  - `java.util.Map`: Maps of any supported type, keys can be Strings or Enums

- **Nested Collections**:
  - **List of Lists**: Nested lists (e.g., `List<List<T>>`).
  - **Map of Maps**: Nested maps (e.g., `Map<String, Map<String, T>>`).

#### Important Notes:
- When using Collections, `Base` objects are compared by their keys (i.e., final fields). To change this behavior, you must implement custom `equals()` and `hashCode()` methods in your class.

#### Keys
Mongoman uses the combination of all **final** fields in an object as the key

Keys are used to load and save objects, and must be unique across the collection

Uniqueness is enforced on the set of key fields, not on individual fields.

#### Working with nested objects
Mongoman allows the usage of nested Base classes

``` public Class Door extends Base {...}```
``` 
public Class Car extends Base {
    Door door;
    ....
}
```


Only the keys [final fields] of the nested objects will be stored in the parent object, if you want all fields to be stored add **FullSave** annotation

``` 
@Kind("car")
public Class Car extends Base {
    @FullSave()
    Door door;
    ....
    
    public Car(...) {
        super();
        ....
    }
}
```

#### Shallow Objects
It is possible to enforce that an object will never get saved in its own collection.
This can be common with nested objects that are fully saved in parent

```
@Kind(value = "door", shallow = true)
public Class Door extends Base {
    ....
    
    public Door(...) {
        ....
    }
}
```

```
    Door d = new Door(...)
    ....
    d.save();           // this will always fail because the object is defined as shallow
```


#### Using references
It is possible have 2 classes referencing each other, just make sure to set the @Reference annotation on one of them to avoid cycles while loading.
@Reference objects will not get loaded when fully loading an object with **.load(true)**

``` 
@Kind("car")
public Class Car extends Base {
    Door door;
    ....
    
    public Car(...) {
        ....
    }
}

@Kind("door")
public Class Door extends Base {
    @Reference
    Car car;
    ....
    
    public Door(...) {
        ....
    }
}
```

### Options
Use ***@Options*** annotation when defining a class to do further tweeking
***ignoreNull*** does store null fields into database when saving
***ignoreUnknownProperties*** remove extra/obsolete properties found in database object and are not defined in the class
```
@Kind("door")
@Options(ignoreNull = true, ignoreUnknownProperties = true)
public Class Door extends Base {
    ....
    
    public Door(...) {
        ....
    }
}
```



#### Query
You can query the database using the **Query** class. All mongodb filters are supported, except for the `NOT` operator.

```
    MongoClient mongoClient = new MongoClient(....);
    Datastore store = new Datastore(mongoClient, ...);
    Datastore.setDefaultService(store);

    Filter _gt = query.createFilter("name", Query.FilterOperator.EQUALS, "....");
    Filter _ni = query.createFilter("type", Query.FilterOperator.NOT_IN, new int[]{1, 3});
    Filter _eq = query.createFilter("door.type", Query.FilterOperator.EQUALS, 2);
    Filter _rx = query.createFilter("door.tag, Query.FilterOperator.REGEX, "*alloy");
    _rx.options("i");

    Filter _and = query.createFilter(Query.FilterOperator.AND, _gt, _ni, _eq, _rx);

    Query<Car> query = new Query<>(Car.class);
    query.setFilter(f);
        
    Cursor<Car> cursor = q.execute();

    while(cursor.hasNext()) {
        Car car = cursor.next();
        ...
    }
```

Base objects can be used in queries, in this case it will be translated into their key
```
    Filter _eq = new Filter("door", Query.FilterOperator.EQUALS, door0);
```

#### Unique Fields
You can mark some fields as Unique by adding the @Unique annotation to ensure uniqueness across the DB

``` 
@Kind("car")
public Class Car extends Base {
    Door door;

    @Unique
    long code;
    ....
}
```

### Watch

Use `Watch` to monitor real-time changes in a MongoDB collection, specifically inserts or updates.

#### Usage Example:

```java
// Initialize the MongoDB datastore
Datastore store = new Datastore(mongoClient, "myDatabase");
Datastore.setDefaultService(store);

// Create a watch to monitor insert and update operations on the User collection
Watch<User> userWatch = new Watch<>(User.class, Watch.WatchMode.UPDATE_REPLACE);

// Iterate through the change stream
while (userWatch.hasNext()) {
    User changedUser = userWatch.next();
    System.out.println("Detected change in user: " + changedUser.name);
}
```

#### Atomicty / Transactions
Currently not implemented, will be included in future versions

#### More Examples
Additional examples are available in the test section.
