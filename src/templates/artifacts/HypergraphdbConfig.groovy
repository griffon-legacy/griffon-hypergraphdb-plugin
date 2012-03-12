database {
    configuration {
        // any property that can be set on org.hypergraphdb.HGConfiguration, for example
        skipOpenedEvent = true
    }
}
environments {
    development {
        database {
            location = '@griffon.project.key@-dev'
        }
    }
    test {
        database {
            location = '@griffon.project.key@-test'
        }
    }
    production {
        database {
            location = '@griffon.project.key@-prod'
        }
    }
}
