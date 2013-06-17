
Hypergraphdb support
--------------------

Plugin page: [http://artifacts.griffon-framework.org/plugin/hypergraphdb](http://artifacts.griffon-framework.org/plugin/hypergraphdb)


The Hypergraphdb plugin enables lightweight access to [Hypergraphdb][1] databases.
This plugin does NOT provide domain classes nor dynamic finders like GORM does.

Usage
-----
Upon installation the plugin will generate the following artifacts in `$appdir/griffon-app/conf`:

 * HypergraphdbConfig.groovy - contains the database definitions.
 * BootstrapdbHypergraphdb.groovy - defines init/destroy hooks for data to be manipulated during app startup/shutdown.

A new dynamic method named `withHyperGraph` will be injected into all controllers,
giving you access to an `org.hypergraphdb.HyperGraph` object, with which you'll be able
to make calls to the database. Remember to make all database calls off the EDT
otherwise your application may appear unresponsive when doing long computations
inside the EDT.

This method is aware of multiple databases. If no database name is specified when calling
it then the default database will be selected. Here are two example usages, the first
queries against the default database while the second queries a database whose name has
been configured as 'internal'

    package sample
    class SampleController {
        def queryAllDataDatabases = {
            withHyperGraph { databaseName, proxy -> ... }
            withHyperGraph('internal') { databaseName, proxy -> ... }
        }
    }
    
This method is also accessible to any component through the singleton `griffon.plugins.hypergraphdb.HypergraphdbConnector`.
You can inject these methods to non-artifacts via metaclasses. Simply grab hold of a particular metaclass and call
`HypergraphdbEnhancer.enhance(metaClassInstance, hypergraphdbProviderInstance)`.

Configuration
-------------
### Dynamic method injection

The `withHyperGraph()` dynamic method will be added to controllers by default. You can
change this setting by adding a configuration flag in `griffon-app/conf/Config.groovy`

    griffon.hypergraphdb.injectInto = ['controller', 'service']

### Events

The following events will be triggered by this addon

 * HypergraphdbConnectStart[config, databaseName] - triggered before connecting to the database
 * HypergraphdbConnectEnd[databaseName, database] - triggered after connecting to the database
 * HypergraphdbDisconnectStart[config, databaseName, database] - triggered before disconnecting from the database
 * HypergraphdbDisconnectEnd[config, databaseName] - triggered after disconnecting from the database

### Multiple Databases

The config file `HypergraphdbConfig.groovy` defines a default database block. As the name
implies this is the database used by default, however you can configure named databases
by adding a new config block. For example connecting to a database whose name is 'internal'
can be done in this way

    databases {
        internal {
            location = '/path/to/database'
        }
    }

This block can be used inside the `environments()` block in the same way as the
default database block is used.

### Example

A trivial sample application can be found at [https://github.com/aalmiray/griffon_sample_apps/tree/master/persistence/hypergraphdb][2]


Testing
-------
The `withHyperGraph()` dynamic method will not be automatically injected during unit testing, because addons are simply not initialized
for this kind of tests. However you can use `HypergraphEnhancer.enhance(metaClassInstance, hypergraphProviderInstance)` where 
`hypergraphProviderInstance` is of type `griffon.plugins.hypergraph.HypergraphProvider`. The contract for this interface looks like this

    public interface HypergraphdbProvider {
        Object withHyperGraph(Closure closure);
        Object withHyperGraph(String databaseName, Closure closure);
        <T> T withHyperGraph(CallableWithArgs<T> callable);
        <T> T withHyperGraph(String databaseName, CallableWithArgs<T> callable);
    }

It's up to you define how these methods need to be implemented for your tests. For example, here's an implementation that never
fails regardless of the arguments it receives

    class MyHypergraphdbProvider implements HypergraphdbProvider {
        Object withHyperGraph(String databaseName = 'default', Closure closure) { null }
        public <T> T withHyperGraph(String databaseName = 'default', CallableWithArgs<T> callable) { null }
    }

This implementation may be used in the following way

    class MyServiceTests extends GriffonUnitTestCase {
        void testSmokeAndMirrors() {
            MyService service = new MyService()
            HypergraphdbEnhancer.enhance(service.metaClass, new MyHypergraphProvider())
            // exercise service methods
        }
    }


[1]: http://www.hypergraphdb.org/index
[2]: https://github.com/aalmiray/griffon_sample_apps/tree/master/persistence/hypergraphdb

