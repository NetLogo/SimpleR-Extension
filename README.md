
# Netlogo Simple R Extension

This NetLogo extension allows you to run R code from within NetLogo.

## Building

Make sure your sbt is at least at version 0.13.6

Run `sbt package`.

If compilation succeeds, `sr.jar` will be created, and the required dependencies will be copied to the root of the repository.  Copy all the `jar` files and `rext.r` from the repository root to a `sr` directory inside your NetLogo `extensions` directory.

## Using

To run R code you must install R and have the `R` executable on your `PATH`.
You can download R from [their site](https://www.r-project.org/).

To use this extension, you must first include it at the top of your NetLogo model code in an `extensions` declaration.

```netlogo
extensions [
  sr
  ; ... your other extensions
]
```

You must then initialize the R environment with `sr:setup`. This only needs to be done once per session.
Any subsequent calls will reset your R environment.

Here's an example to get you started:

```netlogo
observer> sr:setup
;; sr:runresult evaluates R statements and returns the result back to NetLogo
observer> show sr:runresult "2 + 2"
observer: 4
;; sr:run runs R code
observer> sr:run "cat('hello world')"
hello world
;; any standard output gets forwarded to the command center output
;; sr:set sets R variables to values from NetLogo
observer> ask patch 0 0 [ set pcolor red ]
observer> sr:set "center_patch_color" [pcolor] of patch 0 0
observer> show sr:runresult "center_patch_color"
observer: 15 ;; the NetLogo representation of the color red
```

See the documentation for each of the particular primitives for details on, for instance, how to multi-line statements and how object type conversions work.

The extension also includes an interactive R console/REPL that is connected to the same R environment as the main window's NetLogo environment.
It is useful for executing longer blocks of R code or quickly examining or modifying R values.
This console can be opened via the menu bar under the SimpleR heading.

### Error handling

R errors will be reported in NetLogo as "Extension exceptions". For instance, this code:

```netlogo
sr:run "stop('hi')"
```

will result in the NetLogo error "Extension exception: hi".

## Primitives


### `sr:setup`

```NetLogo
sr:setup
```


Create the R session that this extension will use to execute code.
This command *must* be run before running any other R extension primitive.
Running this command again will shutdown the current R environment and start a new one.



### `sr:run`

```NetLogo
sr:run R-statement
```



Runs the given R statements in the current session.
To make multi-line R code easier to run, this command will take multiple strings, each of which will be interpreted as a separate line of R code.
For instance:

```NetLogo
(sr:run
  "domain <- seq(-3.14, 3.14, 0.01)"
  "range <- sin(domain)"
  "png('my_file.png')"
  "plot(domain, range, "
  "     pch = 20,"
  "     main = 'y = sin(x)',"
  "     xlab = 'x',"
  "     ylab = 'y')"
  "dev.off()"
)
```

`sr:run` will wait for the statements to finish before continuing.
If you have long-running R code, NetLogo may freeze for a bit while it runs.



### `sr:runresult`

```NetLogo
sr:runresult R-expression
```


Evaluates the given R expression and reports the result.
`rs:runresult` attempts to convert from R data types to NetLogo data types.


Numbers, strings, and booleans convert as you would expect, except for outliers like Infinity and NaN which will be converted into the strings 'Inf' and 'NaN', respectively.


R vectors and R lists will be converted to NetLogo lists. NA values will be converted into the string 'NA'.


R matrices will be flattened into one-dimensional lists using column-major order.
If you want to convert a matrix into a list of lists before sending it to NetLogo, use the R `asplit` command.
To convert into a list of column lists, use `asplit(<matrix>, 1)`; for a list of row lists, use `asplit(<matrix>, 2)`.


An R DataFrame will be converted into a list of lists, where the first item in each sublist is the name of the column and the second item is a list containing all that row data.
For example, the first 6 rows of the `iris` dataset will be converted into NetLogo like so:
```NetLogo
[
  ["Sepal.Length" [5.1 4.9 4.7 4.6 5 5.4]]
  ["Sepal.Width" [3.5 3 3.2 3.1 3.6 3.9]]
  ["Petal.Length" [1.4 1.4 1.3 1.5 1.4 1.7]]
  ["Petal.Width" [0.2 0.2 0.2 0.2 0.2 0.4]]
  ["Species" ["setosa" "setosa" "setosa" "setosa" "setosa" "setosa"]]
]
```

Other objects will be converted to a string representation if possible and and may throw an error if not.



### `sr:set`

```NetLogo
sr:set variable-name value
```


Sets a variable in the R session with the given name to the given NetLogo value.
NetLogo objects will be converted to R objects as expected.


Note that lists in NetLogo are converted into lists in R. You will often want to use the R function `unlist` to convert these lists into R vectors.

```NetLogo
sr:set "x" 42
sr:run "print(x)" ;; prints `[1] 42` to the command center
sr:set "y" [1 2 3]
sr:run "print(typeof(y))" ;; prints `[1] "list"` to the command center
sr:run "print(typeof(unlist(y)))" ;; prints `[1] "double"` to the command center
sr:run "print(unlist(y))" ;; prints `[1] 1, 2, 3` to the command center
show sr:runresult "y" ;; reports [1 2 3]
```

Agents are converted into lists with named elements for each agent variable.

Agentsets are converted into a list of the above lists. To convert this list of lists into a dataframe, use the following:

```R
my_data_frame <- as.data.frame(do.call(rbind, <agentset-list-of-lists>))
```

For example:
```NetLogo
breed [goats goat]
goats-own [energy ]
create-goats 2 [ set color 75 ]
ask goat 0 [ set energy 42 set xcor 5]
ask goat 1 [ set energy -42 set xcor -5]

sr:set "goat" goat 0
sr:run "print(typeof(goat))" ;; prints `[1] "list"` to the command center
sr:run "print(goat)"
;; Should output:
;; $WHO
;; [1] 0
;;
;; $COLOR
;; [1] 75
;; (etc.)

sr:set "goats_list_of_lists" goats
sr:run "goats_data_frame <- as.data.frame(do.call(rbind, goats_list_of_lists))"
sr:run "print(goats_data_frame)"
;; Should output:
;;   WHO COLOR HEADING XCOR YCOR   SHAPE LABEL LABEL-COLOR BREED HIDDEN? SIZE
;; 1   0    75      82    5    0 default               9.9 GOATS   FALSE    1
;; 2   1    75     200   -5    0 default               9.9 GOATS   FALSE    1
;;   PEN-SIZE PEN-MODE ENERGY
;; 1        1       up     42
;; 2        1       up    -42
;;
```

Agents with variables containing references to agentsets will have those variables converted into the string representation of that agentset.



## Transitioning from the old R extension

Most all of the functionality from the old R extension remains in SimpleR, though much of the specifics of how data is passed between NetLogo and R has been changed.

For example, `sr:runresult` can be used instead of the old `r:get`, but the two handle dataframes differently.
`sr:set` can replace `r:put`, but note that `sr:set` does not automatically convert lists of similar types into vectors. Use the R function `unlist()` to do so manually.
`sr:run` should be able to be a drop-in replacement for `r:eval`, though there is no counterpart for the experimental primitive `r:__evaldirect`.
To clear the R environment, call `sr:setup` instead of `r:clear` or `r:clearLocal`.

Displaying a plot in a window is not supported, but plotting to image devices is. See the `sr:run` documentation or the `plotting.nlogo` demo file.

There are no longer the same convenience methods to easily convert NetLogo variables into helpful R data structures. Perhaps this can be an area of improvement in the future.

