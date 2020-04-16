# Minimalistic mongodb framework

Mongoman is an easy to use framework that helps with mapping your java classes to mongodb objects


# Installation

Download dist/mongoman.jar and include it in your libraries (or compile your own version)

Must also include mongo-java-driver in your project

# Usage

#### Loading and storing objects into mongodb
- Implement a class that **extends** mongoman.Base
- Your class will represent one unique collection in the database
- Only **non-static public** fields get stored
- **non-static final public** fields are used as keys to load and store the objects and marked as unique index (they cannot repeat)
- Use load() and store() to load and save the object
- Your class must implement a 0-argument constructor

##### Example

```
class Car extends Base {
    public int myInteger;               // stored in db
    public double[] myDoubleArray;      // stored in db
   
    public final String myId;   // saved in db, also unique across collection
    private double priv;        // this will not get stored in db

    public static String collectionName = "car"

    public Car(...) {
        super(collectionName);
        myId = ....
        .....
    }
    
    // must implement a 0-argument constructor
    public Car() {
        super(collectionName);
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
Not all fields can be serialized into mongodb, the following are the ones guaranteed to work

- All primitive types (int, byte, float, ...)
- All primitive type arrays (int[], byte[], float[], ....)
- String
- java.util.List
- java.util.Set
- java.util.Map
- java.util.Date
- Any class that extends mongoman.Base
- An array of any class that extends mongoman.Base

Note that using when using java.util.Set, 2 Base objects are considered equal if their keys are equal (final fields)
if you wish to change this behavior you must implement "equals" and "hashCode" for your class

#### Using nested objects
Mongoman allows the usage of nested Base classes

``` public Class Door extends Base {...}```
``` 
public Class Car extends Base {
    Door door;
    ....
}
```

When loading and saving the parent object, Mongoman will automatically load / save any nested objects in their respective collections, while only storing "keys" in the original object

If you want the full object to be saved with parent (this might be useful for querying). Set the **fullSave** variable to true when intializating
``` 
public Class Car extends Base {
    Door door;
    ....
    
    public Car(...) {
        super("car", true);
        ....
    }
}
```


#### Query
You can query the database using the **Query** class, all mongodb filters are supported

```
    MongoClient mongoClient = new MongoClient(....);
    Datastore store = new Datastore(mongoClient, ...);
    Datastore.setDefaultService(store);

    Filter _gt = new Filter("name", Query.FilterOperator.EQUALS, "....");
    Filter _ni = new Filter("type", Query.FilterOperator.NOT_IN, new int[]{1, 3});
    Filter _eq = new Filter("door.type", Query.FilterOperator.EQUALS, 2);

    Filter _and = new Filter(Query.FilterOperator.AND, _gt, _ni, _eq);

    Query<Car> query = new Query<>(Car.class);
    query.setFilter(f);
        
    Cursor<Car> cursor = q.execute();

    while(cursor.hasNext()) {
        Car car = cursor.next();
        ...
    }
```

#### Atomicty / Transactions
Currently not implemented, will be included in future versions
