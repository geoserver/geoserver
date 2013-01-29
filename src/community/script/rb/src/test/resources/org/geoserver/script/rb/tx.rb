def before(req, context)
  context["before"] = true;
end

def preInsert(inserted, req, context)
  context["preInsert"] = true;
end

def postInsert(inserted, req, context)
  context["postInsert"] = true;
end

def preUpdate(updated, props, req, context)
  context["preUpdate"] = true;
end

def postUpdate(inserted, props, req, context)
  context["postUpdate"] = true;
end

def preDelete(deleted, req, context)
  context["preDelete"] = true;
end

def postDelete(deleted, req, context)
  context["postDelete"] = true;
end

def preCommit(req, context)
  context["preCommit"] = true;
end

def postCommit(req, res, context)
  context["postCommit"] = true;
end

def abort(req, res, context)
  context["abort"] = true;
end
