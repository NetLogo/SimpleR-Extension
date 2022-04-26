rExtensionUserDirPath <- commandArgs(trailingOnly = TRUE)[2]
rExtensionLibraryPath <- file.path(rExtensionUserDirPath, paste0("r-", R.version$major, ".", R.version$minor, "-library"))

.libPaths(c(.libPaths(), rExtensionLibraryPath))

suppressPackageStartupMessages(require("rjson"))

# In
quit_msg <- -1
stmt_msg <- 0
expr_msg <- 1
assn_msg <- 2
expr_stringified_msg <- 3
heartbeat_request_msg <- 4

# Out
succ_msg <- 0
err_msg <- 1
heartbeat_response_msg <- 4

# The environment we are working in should have the same parent as the current environment.
# That way we have all of the default imports but none of the variables we define in *this* file's environment.
env <- new.env(parent = parent.env(environment()))

eval_wrapper <- function(s) {
  eval(parse(text=s), envir=env)
}

send_error <- function(sock, message, longMessage) {
  err_msg <- list(type = err_msg,
                  body = list(message = message,
                              longMessage = paste(longMessage, "\n")))
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
  assign(varName, value, envir=env)
  out_msg <- list(type = succ_msg,
                  body = "")
  writeLines(toJSON(out_msg), sock)
}

handle_heartbeat <- function(sock) {
  out_msg <- list(type = heartbeat_response_msg)
  writeLines(toJSON(out_msg), sock)
}

handle_quit <- function(sock) {
  out_msg <- list(type = succ_msg)
  writeLines(toJSON(out_msg), sock)
}

server <- function() {
  port <- strtoi(commandArgs(trailingOnly=TRUE)[1])
  sock <- socketConnection(host="localhost",
                           port=port,
                           blocking=TRUE,
                           server=TRUE,
                           open="r+")

  active <- TRUE
  while(active) {
    tryCatch({
      msg_line <- readLines(sock, 1)
      if (length(msg_line) != 0) {
        msg_parsed <- fromJSON(json_str=msg_line, simplify=FALSE)
        msg_type <- msg_parsed$type

        if (msg_type == heartbeat_request_msg) {
          handle_heartbeat(sock)

        } else if (msg_type == stmt_msg) {
          handle_statememt(sock, msg_parsed$body)

        } else if (msg_type == expr_msg) {
          handle_expression(sock, msg_parsed$body)

        } else if (msg_type == assn_msg) {
          handle_assignment(sock, msg_parsed$body)

        } else if (msg_type == expr_stringified_msg) {
          handle_expression_stringified(sock, msg_parsed$body)

        } else if (msg_type == quit_msg) {
          active <- FALSE
          handle_quit(sock)

        } else {
          send_error("Bad message type: ", toString(msg_type), "")
        }
      }
    },
    error=function(e) {
      send_error(sock, e$message, e$message)
    },
    warning=function(w) {
      writeLines("warning")
      writeLines(w$message)
    })
  }
  close(sock)
}

server()
