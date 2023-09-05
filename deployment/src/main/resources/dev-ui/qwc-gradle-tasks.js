import { LitElement, html, css} from 'lit';
import { columnBodyRenderer } from '@vaadin/grid/lit.js';
import { notifier } from 'notifier';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/grid';
import '@vaadin/text-field';

/**
 * This component shows gradle tasks.
 */
export class QwcGradleTasks extends LitElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
       :host {
            display: flex;
            flex-direction: column;
            gap: 10px;
            height: 100%;
        }

        .tasks-table {
          padding-bottom: 10px;
        }

        code {
          font-size: 85%;
        }

        vaadin-button {
            cursor:pointer;
        }
        `;

    static properties = {
         _tasks: {state: true},
    };

    connectedCallback() {
        super.connectedCallback();
        this.jsonRpc.getTasks()
            .then(jsonResponse => {
                console.log(jsonResponse);
                this._tasks = jsonResponse.result.tasks;

            }).catch(console.log);
    }

    disconnectedCallback() {
        super.disconnectedCallback();
    }

    render() {
        if (this._tasks){
            return this._renderTasks();
        } else {
            return html`<span>Loading tasks...</span>`;
        }
    }

    _renderTasks(){

        return html`
                <vaadin-grid .items="${this._tasks}" class="tasks-table" theme="no-border">
                    <vaadin-grid-column auto-width
                        header="Tasks"
                        ${columnBodyRenderer(this._taskRenderer, [])}
                        resizable>
                    </vaadin-grid-column>
                </vaadin-grid>
                `;
    }

    _taskRenderer(task) {
      return html`
        <vaadin-button theme="small" @click=${() => this._executeTask(task.name)}>
            <vaadin-icon icon="font-awesome-solid:bolt"></vaadin-icon>
            Execute
        </vaadin-button>
        <code>${task.name}</code>
    `;
    }

    _executeTask(id) {
        this.jsonRpc.executeTask(id).then(jsonResponse => {
            if (jsonResponse.result.success) {
                notifier.showSuccessMessage(jsonResponse.result.message);
            } else {
                notifier.showErrorMessage(jsonResponse.result.message);
            }
        });
    }


}
customElements.define('qwc-gradle-tasks', QwcGradleTasks);