## Data Pre-Processing Tasks for Knowledge Base Creation

This project consists of different pre-processing tasks required on input file(s) before they can be used for respective Knowledge Base Creation (KBC).

### Technologies Used

These are all `Scala` based scripts / programs each representing individual pre-processing tasks built using `sbt`

__Dependencies__

- `cats` - for typeclasses & data types
- `monix` - for observables, non-blocking Task and parallel processing; in other words for all the side-effects
- `pureconfig` - for typed configuration (if and when required)

__how to run__

- make relevant changes to `application.conf` for the respective module (like associatekbc or domain)
- `sbt run` command will ask you to select the `App` you want to run

### TODO

- [ ] replace current multiple main classes by multiple `sbt` projects
- [ ] better way to do parallel & non-blocking IO for huge files without non-daemonic threads
- [ ] TODOS from Domain
  - [ ] refactor regex(s) and keep them in one place
  - [ ] introduce free monads for actions and make the current implementation of parsing text as part of an interpretor there by making the whole parsing action extensible to any kind of input data
  - [ ] once a free monad structure is introduced for domain objects, create new interpretors with Akka Stream or FS2 as effects to see if they help improve the performance

_there will be bugs_
