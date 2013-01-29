def before(req, context) {
  context["before"] = true;
}

def preInsert(inserted, req, context) {
  context["preInsert"] = true;
}

def postInsert(inserted, req, context) {
  context["postInsert"] = true;
}

def preUpdate(updated, props, req, context) {
  context["preUpdate"] = true;
}

def postUpdate(updated, props, req, context) {
  context["postUpdate"] = true;
}

def preDelete(deleted, req, context) {
  context["preDelete"] = true;
}

def postDelete(deleted, req, context) {
  context["postDelete"] = true;
}

def preCommit(req, context) {
  context["preCommit"] = true;
}

def postCommit(req, res, context) {
  context["postCommit"] = true;
}

def abort(req, res, context) {
  context["abort"] = true;
}
