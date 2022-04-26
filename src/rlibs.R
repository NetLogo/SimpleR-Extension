# if a user doesn't have write access to their R install location, typical on Windows, we need a fallback.  We
# don't really want to mess with their actuall R stuff, though, so we'll place the package needed for this
# extension inside the extension's user directory.  That'll keep it separated and not cause trouble with anything
# else the user is doing with R.  -Jeremy B April 2022

rExtensionUserDirPath <- commandArgs(trailingOnly = TRUE)[1]
rExtensionLibraryPath <- file.path(rExtensionUserDirPath, paste0("r-", R.version$major, ".", R.version$minor, "-library"))

.libPaths(c(.libPaths(), rExtensionLibraryPath))

if (!suppressPackageStartupMessages(require("rjson"))) {
  if (!dir.exists(rExtensionLibraryPath)) {
    dir.create(rExtensionLibraryPath)
  }
  suppressPackageStartupMessages(
    install.packages(
      "rjson"
    , repos = "http://cran.us.r-project.org"
    , quiet = TRUE
    , lib = rExtensionLibraryPath
    )
  )
}
