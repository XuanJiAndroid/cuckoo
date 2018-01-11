package com.guoxiaoxing.cuckoo.plugin

class CuckooExtension {

    List<String> includeJarFilter = new ArrayList<String>()
    List<String> excludeJarFilter = new ArrayList<String>()
    List<String> ajcArgs = new ArrayList<>();

    public CuckooExtension includeJarFilter(String... filters) {
        if (filters != null) {
            includeJarFilter.addAll(filters)
        }

        return this
    }

    public CuckooExtension excludeJarFilter(String... filters) {
        if (filters != null) {
            excludeJarFilter.addAll(filters)
        }

        return this
    }

    public CuckooExtension ajcArgs(String... ajcArgs) {
        if (ajcArgs != null) {
            this.ajcArgs.addAll(ajcArgs)
        }
        return this
    }
}

