def isoHandlers = new iso19139.Handlers(handlers, f, env)

isoHandlers.addDefaultHandlers()

handlers.roots("che:legislationInformation")