if (!suppressPackageStartupMessages(require("rjson"))) {suppressPackageStartupMessages(install.packages(rjson))}

# In
stmt_msg <- 0
expr_msg <- 1
assn_msg <- 2
expr_stringified_msg <- 3

# Out
succ_msg <- 0
err_msg <- 1

env <- new.env(parent = baseenv())

eval_wrapper <- function(s) {
  eval(parse(text=s), envir=env)
}

send_error <- function(sock, message, cause) {
  err_msg <- list(type = err_msg,
                  body = list(message = message,
                              cause = cause))
  writeLines(toJSON(err_msg), sock)
}

handle_statememt <- function(sock, body) {
  eval_wrapper(body)
  out_msg <- list(type = succ_msg,
                  body = "")
  writeLines(toJSON(out_msg), sock)
}

handle_expression <- function(sock, body) {
  res <- eval_wrapper(body)
  out_msg <- list(type = succ_msg,
                  body = res)
  writeLines(toJSON(out_msg), sock)
}

handle_expression_stringified <- function(sock, body){
  res <- toString(eval_wrapper(body))
  out_msg <- list(type = succ_msg,
                  body = res)
  writeLines(toJSON(out_msg), sock)
}

handle_assignment <- function(sock, body) {
  varName <- body$varName
  value   <- body$value
  assign(varName, value, envir=globalenv())
  out_msg <- list(type = succ_msg,
                  body = "")
  writeLines(toJSON(out_msg), sock)
}

server <- function(){
  port <- strtoi(commandArgs(trailingOnly=TRUE)[1])
  sock <- socketConnection(host="localhost",
                           port=port,
                           blocking=TRUE,
                           server=TRUE,
                           open="r+")
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

      if (msg_type == stmt_msg) {
        handle_statememt(sock, msg_parsed$body)
      } else if (msg_type == expr_msg) {
        handle_expression(sock, msg_parsed$body)
      } else if (msg_type == assn_msg) {
        handle_assignment(sock, msg_parsed$body)
      } else if (msg_type == expr_stringified_msg) {
        handle_expression_stringified(sock, msg_parsed$body)
      } else {
        send_error("Bad message type" + toString(msg_type), "")
      }
    },
     error=function(e) {
       writeLines("error")
       writeLines(e$message)
       send_error(sock, e$message, "TODO")
     },
    warning=function(w) {
      writeLines("warning")
      writeLines(w$message)
    })
  }
  close(sock)
}

server()
