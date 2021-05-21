simpleR_internal_server <- function(){
  simpleR_internal_port <- strtoi(commandArgs(trailingOnly=TRUE)[1])
  simpleR_internal_socket <- socketConnection(host="localhost", port=simpleR_internal_port, blocking=TRUE, server=TRUE, open="r+")
  writeLines("Listening...")
  writeLines(toString(simpleR_internal_port))
  while(TRUE) {
    tryCatch({
      simpleR_internal_msg_line <- readLines(simpleR_internal_socket, 1)
      if (length(simpleR_internal_msg_line) == 0) { # Check if connection is broken. When the connection is lost, readLines repeatedly returns 0 length data
        break
      }
      # writeLines(toString(eval(parse(text=simpleR_internal_msg_line))), simpleR_internal_socket)
      writeLines("{\"type\": 0, \"body\": \"\"}", simpleR_internal_socket)
    },
     error=function(e) {
       writeLines("error")
       writeLines(e$message)
       if(e$message == "ignoring SIGPIPE signal"){
         break
       }
     },
    warning=function(w) {
      writeLines("warning")
      writeLines(w$message)
    })
  }
  close(simpleR_internal_socket)
}

simpleR_internal_server()
