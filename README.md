# blockchain

General purpose blockchain with dynamic proof of work, implemented in Clojure/lisp

## Usage
```lein deps```

Open up your favorite repl
```lein repl```

Start mining:
```(generate-next-block! "My data" @block-chain)```

Difficulty should dynamically change if you include more miners. This can be achieved by instantiating a few go-loops.

## License

Copyright © 2020 Peder August Fasting

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
