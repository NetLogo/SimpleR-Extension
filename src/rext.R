if (!require("rjson")) {install.packages(rjson)}

## The only way I was able to find to let the user create new variables and
## then use them in a different command is to have the eval() interact
## with the global scope (globalenv()). Therfore all global variables
## need to be named something that unambigiously tells the user not to create
## a variable with the same name/override them, like prefixing them all
## with simpleR_internal_.

# In
simpleR_internal_stmt_msg <- 0
simpleR_internal_expr_msg <- 1
simpleR_internal_assn_msg <- 2

# Out
simpleR_internal_succ_msg <- 0
simpleR_internal_err_msg <- 1

simpleR_internal_assn_tmp <- NA

simpleR_internal_eval_wrapper <- function(s) {
  eval(parse(text=s), envir=globalenv())
}

simpleR_internal_send_error <- function(sock, message, cause) {
  err_msg <- list(type = simpleR_internal_err_msg,
                  body = list(message = message,
                              cause = cause))
  writeLines(toJSON(err_msg), sock)
}

simpleR_internal_handle_statememt <- function(sock, body) {
  simpleR_internal_eval_wrapper(body)
  out_msg <- list(type = simpleR_internal_succ_msg,
                  body = "")
  writeLines(toJSON(out_msg), sock)
}

simpleR_internal_handle_expression <- function(sock, body) {
  res <- simpleR_internal_eval_wrapper(body)
  out_msg <- list(type = simpleR_internal_succ_msg,
                  body = res)
  writeLines(toJSON(out_msg), sock)
}

simpleR_internal_handle_assignment <- function(sock, body) {
  varName <- body$varName
  value   <- body$value
  assign(varName, value, envir=globalenv())
  out_msg <- list(type = simpleR_internal_succ_msg,
                  body = "")
  writeLines(toJSON(out_msg), sock)
}

simpleR_internal_server <- function(){
  port <- strtoi(commandArgs(trailingOnly=TRUE)[1])
  sock <- socketConnection(host="localhost",
                           port=port,
                           blocking=TRUE,
                           server=TRUE,
                           open="r+")
  # writeLines("Listening...")
  # writeLines(toString(port))
  while(TRUE) {
    tryCatch({
      msg_line <- readLines(sock, 1)
      # Check if connection is broken. When the connection is lost, readLines
      # repeatedly returns 0 length data
      if (length(msg_line) == 0) {
        break
      }

      msg_parsed <- fromJSON(json_str=msg_line, simplify=FALSE)
      msg_type <- msg_parsed$type

      writeLines(toString(msg_parsed))

      if (msg_type == simpleR_internal_stmt_msg) {
        simpleR_internal_handle_statememt(sock, msg_parsed$body)
      } else if (msg_type == simpleR_internal_expr_msg) {
        simpleR_internal_handle_expression(sock, msg_parsed$body)
      } else if (msg_type == simpleR_internal_assn_msg) {
        simpleR_internal_handle_assignment(sock, msg_parsed$body)
      } else {
        simpleR_internal_send_error("Bad message type" + toString(msg_type), "")
      }
    },
     error=function(e) {
       writeLines("error")
       writeLines(e$message)
       simpleR_internal_send_error(sock, e$message, "TODO")
     },
    warning=function(w) {
      writeLines("warning")
      writeLines(w$message)
    })
  }
  close(sock)
}

simpleR_internal_server()
